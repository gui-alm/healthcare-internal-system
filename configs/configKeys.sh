#!/bin/bash

while getopts ":c :a" opt; do
  case $opt in
    c)
      echo "Setting up client keys..."
      echo "Inser client name (doctor or patient name):"
      read name
      file=$name
      break;
      ;;
    a)
      echo "Setting up API keys..."
      file=emergency
      break;
      ;;
    \?)
      echo "Invalid option: -$OPTARG"
      exit 1
      ;;
  esac
done

if [ -z "$file" ]; then
  echo "Needs at least one option"
  exit 1
fi

sudo rm -r ~/meditrack
mkdir ~/meditrack
sudo cp -r meditrack/publicKeys ~/meditrack/publicKeys
sudo cp meditrack/$file.priv ~/meditrack
