#!/bin/bash

sudo systemctl enable postgresql
sudo systemctl start postgresql

sudo -u postgres createuser -s -e -P sysadmin
sudo -u postgres createdb -e meditrack_db

sudo cp dbConfigs/postgresql.conf /etc/postgresql/15/main/postgresql.conf
sudo cp dbConfigs/pg_hba.conf /etc/postgresql/15/main/pg_hba.conf

sudo systemctl restart postgresql
