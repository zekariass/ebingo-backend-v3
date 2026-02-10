$filePath = "src\main\java\com\ebingo\backend\payment\service\WalletServiceImpl.java"
$content = Get-Content $filePath

# Insert closing brace after line 574 (index 573)
$newContent = @()
for ($i = 0; $i -lt $content.Length; $i++) {
    $newContent += $content[$i]
    if ($i -eq 573) {
        $newContent += "                });"
    }
}

$newContent | Set-Content $filePath
Write-Host "Added closing brace after line 574"
