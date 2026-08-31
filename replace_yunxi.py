#!/usr/bin/env python3
"""
替换所有 "云析" 为 "云解析"
"""
import os
import glob

# 需要替换的文本
OLD_TEXT = "云析"
NEW_TEXT = "云解析"

# 搜索所有 Kotlin 文件
kt_files = glob.glob("/workspace/YunX/app/src/**/*.kt", recursive=True)

count = 0
for file_path in kt_files:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if OLD_TEXT in content:
            # 替换（避免重复替换 "云解析" 变成 "云云解析"）
            new_content = content.replace(OLD_TEXT, NEW_TEXT)
            # 修正可能的重复：如果出现 "云云解析" 改回 "云解析"
            new_content = new_content.replace("云云解析", NEW_TEXT)
            
            if new_content != content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1
                print(f"✅ 已更新: {file_path}")
    except Exception as e:
        print(f"❌ 错误: {file_path} - {e}")

print(f"\n总计更新了 {count} 个文件")
