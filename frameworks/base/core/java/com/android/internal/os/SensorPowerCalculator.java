/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.os;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.util.SparseArray;

import java.util.List;

public class SensorPowerCalculator extends PowerCalculator {
    private final List<Sensor> mSensors;
    private final double mGpsPowerOn;

    public SensorPowerCalculator(PowerProfile profile, SensorManager sensorManager) {
        mSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        mGpsPowerOn = profile.getAveragePower(PowerProfile.POWER_GPS_ON);
    }

    @Override
    public void calculateApp(BatterySipper app, BatteryStats.Uid u, long rawRealtimeUs,
                             long rawUptimeUs, int statsType) {
        // Process Sensor usage
        final SparseArray<? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
        final int NSE = sensorStats.size();
        for (int ise = 0; ise < NSE; ise++) {
            final BatteryStats.Uid.Sensor sensor = sensorStats.valueAt(ise);
            final int sensorHandle = sensorStats.keyAt(ise);
            final BatteryStats.Timer timer = sensor.getSensorTime();
            final long sensorTime = timer.getTotalTimeLocked(rawRealtimeUs, statsType) / 1000;
            switch (sensorHandle) {
                case BatteryStats.Uid.Sensor.GPS: // uid为GPS
                    app.gpsTimeMs = sensorTime;
                    app.gpsPowerMah = (app.gpsTimeMs * mGpsPowerOn) / (1000*60*60); // 计算GPS功耗
                    break;
                default:
                    final int sensorsCount = mSensors.size();
                    for (int i = 0; i < sensorsCount; i++) { // 统计所有的Sensor情况	
                        final Sensor s = mSensors.get(i);
                        if (s.getHandle() == sensorHandle) {
                            app.sensorPowerMah += (sensorTime * s.getPower()) / (1000*60*60);
                            break;
                        }
                    }
                    break;
            }
        }
    }
}
