package com.bwssystems.HABridge.plugins.somfy;

import com.bwssystems.HABridge.BridgeSettingsDescriptor;
import com.bwssystems.HABridge.DeviceMapTypes;
import com.bwssystems.HABridge.Home;
import com.bwssystems.HABridge.NamedIP;
import com.bwssystems.HABridge.api.CallItem;
import com.bwssystems.HABridge.dao.DeviceDescriptor;
import com.bwssystems.HABridge.hue.MultiCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SomfyHome implements Home  {
    private static final Logger log = LoggerFactory.getLogger(SomfyHome.class);
	private Map<String, SomfyInfo> somfys;
	private Boolean validSomfy;

	public SomfyHome(BridgeSettingsDescriptor bridgeSettings) {
		createHome(bridgeSettings);

	}

	public SomfyInfo getSomfyHandler(String somfyName) {
		return  somfys.get(somfyName);
	}

	public List<SomfyDevice> getDevices() {
		log.debug("consolidating devices for somfy");
		Iterator<String> keys = somfys.keySet().iterator();
		ArrayList<SomfyDevice> deviceList = new ArrayList<>();
		while(keys.hasNext()) {
			String key = keys.next();
			List<SomfyDevice> devices = somfys.get(key).getSomfyDevices();
			deviceList.addAll(devices);
		}
		return deviceList;
	}

	@Override
	public Object getItems(String type) {
		if(validSomfy) {
			if(type.equalsIgnoreCase(DeviceMapTypes.SOMFY_DEVICE[DeviceMapTypes.typeIndex]))
				return getDevices();
		}
		return null;
	}

	@Override
	public String deviceHandler(CallItem anItem, MultiCommandUtil aMultiUtil, String lightId, int intensity, Integer targetBri, Integer targetBriInc, DeviceDescriptor device, String body) {
		String responseString = null;
		if (!validSomfy) {
			log.warn("Should not get here, no somfy hub available");
			responseString = "[{\"error\":{\"type\": 6, \"address\": \"/lights/" + lightId
					+ "\",\"description\": \"Should not get here, no somfy hub available\", \"parameter\": \"/lights/"
					+ lightId + "state\"}}]";
		} else {
			if (anItem.getType() != null && anItem.getType().trim().equalsIgnoreCase(DeviceMapTypes.SOMFY_DEVICE[DeviceMapTypes.typeIndex])) {

				log.debug("executing HUE api request to change activity to Somfy: " + anItem.getItem().getAsString());
				String jsonToPost = anItem.getItem().getAsString();

				//RunActivity anActivity = new Gson().fromJson(url, RunActivity.class);
				SomfyInfo somfyHandler = getSomfyHandler(device.getTargetDevice());
				if(somfyHandler == null) {
					log.warn("Should not get here, no Somfy configured");
					responseString = "[{\"error\":{\"type\": 6, \"address\": \"/lights/" + lightId
							+ "\",\"description\": \"Should not get here, no somfy configured\", \"parameter\": \"/lights/"
							+ lightId + "state\"}}]";
				} else {
					try {
						somfyHandler.execApply(jsonToPost);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				}
			}
		}
		return responseString;
	}

	@Override
	public Home createHome(BridgeSettingsDescriptor bridgeSettings) {
		validSomfy = bridgeSettings.isValidSomfy();
		log.info("Vera Home created." + (validSomfy ? "" : " No Veras configured."));
		if(validSomfy) {
			somfys = new HashMap<String, SomfyInfo>();
			Iterator<NamedIP> theList = bridgeSettings.getSomfyAddress().getDevices().iterator();
			while (theList.hasNext()) {
				NamedIP aSomfy = theList.next();
				somfys.put(aSomfy.getName(), new SomfyInfo(aSomfy, aSomfy.getName()));
			}
		}
		return this;
	}

	@Override
	public void closeHome() {

	}
}
