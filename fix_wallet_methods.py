#!/usr/bin/env python3
"""
Fix R2DBC duplicate key error in WalletServiceImpl by fetching wallet from DB first.
"""

import re

def fix_wallet_service():
    file_path = r"src\main\java\com\ebingo\backend\payment\service\WalletServiceImpl.java"
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Fix debit method
    debit_pattern = r'(public Mono<WalletDto> debit\(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId\) \{\s+log\.info\("Debiting wallet[^"]+", wallet\.getId\(\), amount\);)\s+(String walletCacheKey)'
    
    debit_replacement = r'\1\n\n        // Fetch wallet fresh from DB to ensure R2DBC tracks it as existing entity\n        return walletRepository.findById(wallet.getId())\n                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found with id: " + wallet.getId())))\n                .flatMap(dbWallet -> {\n                    \2'
    
    content = re.sub(debit_pattern, debit_replacement, content, flags=re.DOTALL)
    
    # Replace wallet. with dbWallet. in debit method only
    # Find debit method boundaries
    debit_start = content.find('public Mono<WalletDto> debit(Wallet wallet')
    debit_end = content.find('public Mono<WalletDto> credit(Wallet wallet', debit_start)
    
    if debit_start != -1 and debit_end != -1:
        debit_method = content[debit_start:debit_end]
        # Replace wallet. with dbWallet. but not in method signature
        lines = debit_method.split('\n')
        fixed_lines = [lines[0]]  # Keep signature
        for line in lines[1:]:
            fixed_lines.append(re.sub(r'\bwallet\.', 'dbWallet.', line))
        fixed_debit = '\n'.join(fixed_lines)
        
        # Add extra closing brace before method end
        fixed_debit = re.sub(
            r'(\s+\.onErrorMap\(e -> \{\s+log\.error\("Error debiting wallet", e\);\s+return new RuntimeException\("Failed to debit wallet", e\);\s+\}\);)\s+(\})',
            r'\1\n                });\n    \2',
            fixed_debit
        )
        
        content = content[:debit_start] + fixed_debit + content[debit_end:]
    
    # Fix credit method
    credit_pattern = r'(public Mono<WalletDto> credit\(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType, Long gameId\) \{\s+log\.info\("Crediting wallet[^"]+", wallet\.getId\(\), amount, gameTxnType\);)\s+(// 🧹 Step 1:)'
    
    credit_replacement = r'\1\n\n        // Fetch wallet fresh from DB to ensure R2DBC tracks it as existing entity\n        return walletRepository.findById(wallet.getId())\n                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found with id: " + wallet.getId())))\n                .flatMap(dbWallet -> {\n                    \2'
    
    content = re.sub(credit_pattern, credit_replacement, content, flags=re.DOTALL)
    
    # Replace wallet. with dbWallet. in credit method only
    credit_start = content.find('public Mono<WalletDto> credit(Wallet wallet, BigDecimal amount, GameTxnType gameTxnType')
    credit_end = content.find('public Mono<WalletDto> credit(Long userProfileId', credit_start)
    
    if credit_start != -1 and credit_end != -1:
        credit_method = content[credit_start:credit_end]
        lines = credit_method.split('\n')
        fixed_lines = [lines[0]]  # Keep signature
        for line in lines[1:]:
            fixed_lines.append(re.sub(r'\bwallet\.', 'dbWallet.', line))
        fixed_credit = '\n'.join(fixed_lines)
        
        # Add extra closing brace before method end
        fixed_credit = re.sub(
            r'(\s+\.onErrorMap\(e -> \{\s+log\.error\("Error crediting wallet", e\);\s+return new RuntimeException\("Failed to credit wallet", e\);\s+\}\);)\s+(\})',
            r'\1\n                });\n    \2',
            fixed_credit
        )
        
        content = content[:credit_start] + fixed_credit + content[credit_end:]
    
    # Write back
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print("[OK] Fixed WalletServiceImpl.java")
    print("   - Added wallet fetch from DB in debit() method")
    print("   - Added wallet fetch from DB in credit() method")
    print("   - Replaced wallet references with dbWallet")

if __name__ == '__main__':
    try:
        fix_wallet_service()
    except Exception as e:
        print(f"[ERROR] {e}")
        import traceback
        traceback.print_exc()
