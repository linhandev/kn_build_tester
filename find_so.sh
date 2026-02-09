#!/usr/bin/env bash
FOLDERS=(
  /system/lib64/
  /system/lib/
  /system/lib64/chipset-pub-sdk/
  /system/lib64/chipset-sdk/
  /system/lib64/chipset-sdk-sp/
  /system/lib64/module/
  /system/lib64/appspawn/
  /system/lib64/ndk/
  /system/lib64/platformsdk/
  /system/lib64/media/
  /system/lib64/init/
)

cd /tmp
rm -f ${1}

for dir in "${FOLDERS[@]}"; do
  path="${dir}${1}"
  out=$(hdc shell ls "$path" 2>&1)
  if [[ "$out" == *"$path"* && "$out" != *"No such file"* && "$out" != *"[Fail]"* ]]; then
    printf '\033[42;30m ✓ \033[0m %s\n' "$dir"
    hdc file recv "$path" . &>/dev/null
  else
    printf '\033[41;30m ✗ \033[0m %s\n' "$dir"
  fi
done

ls /tmp/${1}