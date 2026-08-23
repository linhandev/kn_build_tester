#!/usr/bin/env bash
set -ex
#
# Copyright (c) 2026 ByteDance Ltd. and/or its affiliates
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

version="1.0.`date +%s%3`-SNAPSHOT"

SCRIPT_PATH=$(readlink -f "$0")
SCRIPT_DIR=$(dirname "$SCRIPT_PATH")

hostRoot="$SCRIPT_DIR"
devEcoDir="/Applications/DevEco-Studio.app"

echo "hm.dir: $hostRoot"

#获取脚本所在目录
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
HAR_DIR="$SCRIPT_DIR/.local_har"

soName="bytekmp_sample"
echo "soName: $soName, localharPath: $HAR_DIR , harVersion: $version"
./gradlew :launcher:bundleDebugHar "-PsoName=$soName" "-PlocalHarPath=$HAR_DIR" "-PharVersion=$version"
if [ $? -eq 0 ]; then
    echo "./gradlew :launcher:bundleDebugHar success"
else
    echo "./gradlew :launcher:bundleDebugHar failed"
    exit 1
fi

node_path=$(echo "$devEcoDir/Contents/tools/node/bin"| sed 's|//|/|g')
harmony_toolchains=$(echo "$devEcoDir/Contents/sdk/default/openharmony/toolchains" | sed 's|//|/|g')
# 设置环境变量
export PATH=$node_path:$PATH
export PATH=$harmony_toolchains:$PATH
# 查看设备是否链接
device=$("$harmony_toolchains/hdc" list targets)
if [[ "$device" != **"Empty"** ]]; then
  echo "$device"
  "$node_path/node" "$SCRIPT_DIR/patch-so.js" patchSo -l "$HAR_DIR/$soName.har" -p "$hostRoot"
fi
echo "sync success!!"