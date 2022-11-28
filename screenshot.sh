#!/bin/bash

help() {
    echo "A simple script to configure a device for generating Play Store screenshots"
    echo "Syntax: screenshot [-on|off]"
    echo
}

if [ "$1" = '-on' ]
then
    adb shell settings put global sysui_demo_allowed 1
    adb shell am broadcast -a com.android.systemui.demo -e command enter
    adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200
    adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
    adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
    adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
    echo 'Device successfully configured for screenshots.'
elif [ "$1" = '-off' ]
then
    adb shell am broadcast -a com.android.systemui.demo -e command exit
    echo 'Screenshot mode exited.'
else
    help
fi

