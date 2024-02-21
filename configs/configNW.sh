#!/bin/bash

while getopts ":c :o :a :i :d" opt; do
  case $opt in
    c)
      echo "Setting up client machine..."
      configFile=clientNwConfig
      break;
      ;;
    o)
      echo "Setting up outer firewall..."
      configFile=outerFwNwConfig
      break;
      ;;
    a)
      echo "Setting up API server..."
      configFile=APINwConfig
      break;
      ;;
    i)
      echo "Setting up inner firewall..."
      configFile=innerFwNwConfig
      break;
      ;;
    d)
      echo "Setting up DB server..."
      configFile=DBNwConfig
      break;
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      exit 1
      ;;
  esac
done

if [ -z "$configFile" ]; then
  echo "Needs at least one option"
  exit 1
fi

sudo cp nwConfigs/$configFile /etc/network/interfaces

sudo systemctl restart NetworkManager