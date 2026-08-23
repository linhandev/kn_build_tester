#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_DIR="${SCRIPT_DIR}/DanceUIWorkSpace"

mkdir -p "${WORKSPACE_DIR}"

clone_if_missing() {
  local repo_url="$1"
  local repo_name="$2"
  local target_dir="${WORKSPACE_DIR}/${repo_name}"

  if [ -d "${target_dir}/.git" ]; then
    echo "${repo_name} already exists at ${target_dir}, skipping clone"
    return 0
  fi

  git clone "${repo_url}" "${target_dir}"
}

clone_if_missing "git@github.com:bytedance/DanceUI.git" "DanceUI"
clone_if_missing "git@github.com:bytedance/DanceUIRuntime.git" "DanceUIRuntime"
clone_if_missing "git@github.com:bytedance/DanceUIGraph.git" "DanceUIGraph"

cd "${WORKSPACE_DIR}/DanceUI"
bash init.sh
