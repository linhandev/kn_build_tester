#!/bin/sh
# Run uaf_cb.kexe N times on device. Args: [N=1024] [HWASAN_OPTIONS]
set -u
N="${1:-1024}"
OPTS="${2-}"
DIR=/data/local/tmp/hwasan-uaf-cb
export LD_LIBRARY_PATH="$DIR${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
if [ -n "$OPTS" ]; then
  export HWASAN_OPTIONS="$OPTS"
fi
e0=0
hit=0
other=0
i=0
while [ "$i" -lt "$N" ]; do
  i=$((i + 1))
  "$DIR/uaf_cb.kexe" >/dev/null 2>"$DIR/last.err"
  ec=$?
  if [ "$ec" -eq 0 ]; then
    e0=$((e0 + 1))
  elif [ "$ec" -eq 134 ] || [ "$ec" -eq 6 ]; then
    hit=$((hit + 1))
  else
    other=$((other + 1))
    if [ "$other" -le 3 ]; then
      echo "OTHER i=$i ec=$ec" >&2
      tail -5 "$DIR/last.err" >&2
    fi
  fi
  if [ $((i % 64)) -eq 0 ]; then
    echo "i=$i miss=$e0 hit=$hit other=$other"
  fi
done
echo "DONE n=$N miss(exit0)=$e0 hit=$hit other=$other HWASAN_OPTIONS=${OPTS:-<unset>}"
