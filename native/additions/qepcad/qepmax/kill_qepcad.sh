#!/bin/bash

qep_pid=`ps aux | grep bin/qepcad | grep -v bash | grep -v grep | awk '{print $2}'`
echo 'qepcad process id=' $qep_pid
kill -9 $qep_pid
