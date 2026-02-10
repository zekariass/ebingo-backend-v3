$filePath = "src\main\java\com\ebingo\backend\payment\service\WalletServiceImpl.java"
$content = Get-Content $filePath -Raw

# First, add the fetch at the beginning of credit method
$pattern = '(public Mono<WalletDto> credit\(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId\) \{[\r\n\s]+log\.info\("Crediting wallet[^"]+", wallet\.getId\(\), amount, gameTxnType\);[\r\n\s]+)(// 🧹 Step 1:)'
$replacement = '$1' + "`n        // Fetch wallet fresh from DB to ensure R2DBC tracks it as existing entity`n        return walletRepository.findById(wallet.getId())`n                .switchIfEmpty(Mono.error(new ResourceNotFoundException(`"Wallet not found with id: `" + wallet.getId())))`n                .flatMap(dbWallet -> {`n                    $2"

$content = $content -replace $pattern, $replacement

# Now replace wallet. with dbWallet. in the credit method
$pattern2 = '(?s)(public Mono<WalletDto> credit\(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId\).*?)(public Mono<WalletDto> credit\(Long userProfileId)'
$content = $content -replace $pattern2, {
    param($match)
    $creditMethod = $match.Groups[1].Value
    $nextMethod = $match.Groups[2].Value
    
    # Replace wallet. with dbWallet. but not in method signature
    $lines = $creditMethod -split "`n"
    $result = @()
    $firstLine = $true
    foreach ($line in $lines) {
        if ($firstLine) {
            $result += $line
            $firstLine = $false
        } else {
            $result += $line -replace '\bwallet\.', 'dbWallet.'
        }
    }
    
    return ($result -join "`n") + $nextMethod
}

# Add closing brace before next method
$pattern3 = '(\s+\.onErrorMap\(e -> \{[\r\n\s]+log\.error\("Error crediting wallet", e\);[\r\n\s]+return new RuntimeException\("Failed to credit wallet", e\);[\r\n\s]+\}\);[\r\n\s]+\}[\r\n\s]+)(public Mono<WalletDto> credit\(Long userProfileId)'
$replacement3 = '$1' + "                });`n    }`n`n`n    " + '$2'
$content = $content -replace $pattern3, $replacement3

$content | Set-Content $filePath -NoNewline
Write-Host "Fixed credit method"
