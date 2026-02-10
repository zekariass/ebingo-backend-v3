$filePath = "src\main\java\com\ebingo\backend\payment\service\WalletServiceImpl.java"
$content = Get-Content $filePath -Raw

# Replace in debit method (between line 265 and first occurrence of next method)
$pattern = '(?s)(public Mono<WalletDto> debit\(Wallet wallet.*?)(public Mono<WalletDto> credit\(Wallet wallet)'
$content = $content -replace $pattern, {
    param($match)
    $debitMethod = $match.Groups[1].Value
    $creditMethod = $match.Groups[2].Value
    
    # Replace wallet. with dbWallet. in debit method only
    $debitMethod = $debitMethod -replace '\bwallet\.', 'dbWallet.'
    
    return $debitMethod + $creditMethod
}

$content | Set-Content $filePath -NoNewline
Write-Host "Fixed debit method"
