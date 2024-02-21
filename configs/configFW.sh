#!/bin/bash

while getopts ":o :i" opt; do
  case $opt in
    o)
      echo "Setting up outer firewall..."
      IP=192.168.1.195
      PORT=8081
      break;
      ;;
    i)
      echo "Setting up inner firewall..."
      IP=192.168.3.210
      PORT=5432
      break;
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      exit 1
      ;;
  esac
done

if [ -z "$IP" ]; then
  echo "Needs at least one option"
  exit 1
fi

echo "Enabling IP forwarding..."

sudo cp fwConfigs/sysctl.conf /etc/sysctl.conf
sudo sysctl -p

sudo apt install iptables-persistent

echo "Setting Firewall rules..."
sudo iptables -F
sudo iptables -P INPUT DROP
sudo iptables -P OUTPUT DROP
sudo iptables -P FORWARD DROP


sudo iptables -A FORWARD -i eth0 -p tcp --dport $PORT -j ACCEPT
sudo iptables -A FORWARD -i eth1 -p tcp --sport $PORT -j ACCEPT


echo "Setting NAT rules..."

sudo iptables -t nat -F
sudo iptables -t nat -A PREROUTING -i eth0 -p tcp --dport 8081 -j DNAT --to-destination $IP:$PORT
sudo iptables -t nat -A POSTROUTING -o eth1 -j MASQUERADE

sudo sh -c 'iptables-save > /etc/iptables/rules.v4'
sudo systemctl enable netfilter-persistent.service

