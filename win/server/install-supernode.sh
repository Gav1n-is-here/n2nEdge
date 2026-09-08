#!/usr/bin/env bash
set -euo pipefail
script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
test ! -e /opt/n2n/bin/supernode
test ! -e /etc/n2n/supernode.conf
test ! -e /etc/systemd/system/n2n-supernode.service
sudo apt-get update -qq
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends git build-essential autoconf automake libtool pkg-config
build_dir=$(mktemp -d -t n2n-build.XXXXXXXX)
cd "$build_dir"
git clone --branch 3.0 --depth 1 https://github.com/ntop/n2n.git n2n-source-3.0
cd n2n-source-3.0
test "$(git rev-parse HEAD)" = 66f557af97b9c2ad42537516101fd04df2639ef0
./autogen.sh
./configure
make -j1
sudo install -d /opt/n2n/bin /etc/n2n
sudo install -m 755 supernode edge /opt/n2n/bin/
id n2n >/dev/null 2>&1 || sudo useradd --system --no-create-home --shell /usr/sbin/nologin n2n
sudo install -m 644 "$script_dir/supernode.conf" /etc/n2n/supernode.conf
sudo install -m 644 "$script_dir/n2n-supernode.service" /etc/systemd/system/n2n-supernode.service
sudo systemd-analyze verify /etc/systemd/system/n2n-supernode.service
sudo ufw allow 7777/udp comment 'n2n supernode'
sudo systemctl daemon-reload
sudo systemctl enable --now n2n-supernode
sudo systemctl --no-pager --full status n2n-supernode
sudo ss -lnup | grep -E ':7777|:5645'
