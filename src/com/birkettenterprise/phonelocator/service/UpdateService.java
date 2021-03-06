/**
 * 
 *  Copyright 2011 Birkett Enterprise Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 */

package com.birkettenterprise.phonelocator.service;

import java.io.IOException;
import java.util.Vector;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.birkettenterprise.phonelocator.broadcastreceiver.LocationPollerBroadcastReceiver;
import com.birkettenterprise.phonelocator.domain.BeaconList;
import com.birkettenterprise.phonelocator.domain.GpsBeacon;
import com.birkettenterprise.phonelocator.protocol.Session;
import com.birkettenterprise.phonelocator.util.Setting;
import com.birkettenterprise.phonelocator.util.SettingsHelper;
import com.birkettenterprise.phonelocator.util.SettingsManager;

public class UpdateService extends WakefulIntentService {

	public static final String COMMAND = "command";
		
	public static final int UPDATE_LOCATION = 1;
	public static final int SYNCHRONIZE_SETTINGS = 2;

	private static final String TAG = "Phonelocator";
	//private Database mDatabase;
	
	public UpdateService() {
		super("PhonelocatorSerivce");
		//mDatabase = new Database(this);
	}

	@Override
	protected void doWakefulWork(Intent intent) {
		int command = intent.getIntExtra(COMMAND, -1);
			
		//if ((command & UPDATE_LOCATION) == UPDATE_LOCATION) {
		handleUpdateLocation(intent);
	
		if ((command & SYNCHRONIZE_SETTINGS) == SYNCHRONIZE_SETTINGS) {
			handleSynchronizeSettings();
		}
	}
	
	private void handleUpdateLocation(Intent intent) {
		Log.v(TAG, "handleUpdateLocation");
		
		Session session = new Session();
		SettingsManager settingsManager = SettingsManager.getInstance(this, this);
		
		try {
			session.connect();
			session.authenticate(SettingsHelper.getAuthenticationToken(PreferenceManager.getDefaultSharedPreferences(this)));
			
			Bundle bundle = intent.getExtras();
			Location location = (Location) bundle.get(LocationPollerBroadcastReceiver.EXTRA_LOCATION);
			if (location == null) {
				location = (Location) bundle.get(LocationPollerBroadcastReceiver.EXTRA_LASTKNOWN);
			}
			BeaconList beaconList = new BeaconList();
			beaconList.add(new GpsBeacon(location, intent.getStringExtra(LocationPollerBroadcastReceiver.EXTRA_ERROR)));
			session.sendPositionUpdate(beaconList);
			Vector<Setting> settings = session.synchronizeSettings(settingsManager.getSettingsModifiedSinceLastSyncrhonization());
			settingsManager.setSettings(settings);
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.d(TAG,"error synchronizing settings " + e.toString());
		} finally {
			try {
				settingsManager.releaseInstance(this);
				session.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
   

	
	private void handleSynchronizeSettings() {
		
	}
}
