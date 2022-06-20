package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.utils.Logger;

public class ModulePlacementDifferentiated extends ModulePlacement{
	
	protected ModuleMapping moduleMapping;
	protected List<Sensor> sensors;
	protected List<Actuator> actuators;
	protected Map<Integer, Double> currentCpuLoad;	
	/**
	 * Stores the current mapping of application modules to fog devices 
	 */
	protected Map<Integer, List<String>> currentModuleMap;  
	protected Map<Integer, Map<String, Double>> currentModuleLoadMap;
	protected Map<Integer, Map<String, Integer>> currentModuleInstanceNum;
	protected Map<Integer, List<Pair<Integer,String>>> currentModuleMapuser; //ragaa deviceid, list<pair<userid,modulename>>
	
	public ModulePlacementDifferentiated(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, 
			Application application, ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		
		this.setDeviceToModuleMapuser(new HashMap<Integer, List<Pair<Integer, AppModule>>>());  //ragaa
		
		setSensors(sensors);
		setActuators(actuators);
		setCurrentCpuLoad(new HashMap<Integer, Double>());
		setCurrentModuleMap(new HashMap<Integer, List<String>>());
		
		setCurrentModuleMapuser(new HashMap<Integer, List<Pair<Integer,String>>>()); // ragaa 
		
		setCurrentModuleLoadMap(new HashMap<Integer, Map<String, Double>>());
		setCurrentModuleInstanceNum(new HashMap<Integer, Map<String, Integer>>());
		for(FogDevice dev : getFogDevices()){
			getCurrentCpuLoad().put(dev.getId(), 0.0);
			getCurrentModuleLoadMap().put(dev.getId(), new HashMap<String, Double>());
			getCurrentModuleInstanceNum().put(dev.getId(), new HashMap<String, Integer>());
			getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
			// ragaa add user to module map like device to module map or currentmodule map
			getCurrentModuleMapuser().put(dev.getId(), new ArrayList<Pair<Integer, String>>());  //ragaa deviceid, list<pair<userid,modulename>>
		}
		
		mapModules();
		setModuleInstanceCountMap(getCurrentModuleInstanceNum());
		setCurrentModuleLoadMap(getCurrentModuleLoadMap());
		//System.out.println("Module Map at each device "+ getCurrentModuleMap());
		//System.out.println("Module Instance Num at each device "+ getCurrentModuleInstanceNum());
		//System.out.println("Module Load Map at each device "+ getCurrentModuleLoadMap());
		
		for(int i: getCurrentModuleInstanceNum().keySet()) {
			//FogDevice device= (FogDevice) CloudSim.getEntity(i);
			//System.out.println(CloudSim.getEntityName(i) + " " + device.getHost().getTotalMips());
			//System.out.println(CloudSim.getEntityName(i) + " " +getCurrentModuleInstanceNum().get(i) +"--->: " +getCurrentModuleLoadMap().get(i));
		}
	}	

	//******************************************************************************************************************	
	@Override
	protected void mapModules() {
		
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
			int deviceId = CloudSim.getEntityId(deviceName);
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){	
				getCurrentModuleMap().get(deviceId).add(moduleName);				
				getCurrentModuleLoadMap().get(deviceId).put(moduleName, 0.0);
				getCurrentModuleInstanceNum().get(deviceId).put(moduleName, 0);		
			}
			//System.out.println("latencymoduleplacementnear modulemapping"+ getFogDeviceById(deviceId).getName()+ getCurrentModuleMap());
		}
		List<List<Integer>> leafToRootPaths = getLeafToRootPaths();
		//System.out.println("ragaa leaf to root paths  " + leafToRootPaths );
		
		Map<List<Integer>, Double> deadlineleafToRootPaths= new HashMap<>();
		for (List<Integer> x: leafToRootPaths) {
			Integer mobid= x.get(0);
			double deadline = getApplication().getDeadlineInfo().get(mobid);
			deadlineleafToRootPaths.put(x, deadline);			
		}
		Map<List<Integer>, Double> PriodeadlineleafToRootPaths = deadlineleafToRootPaths.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		//System.out.println("ragaa prioritized deadline leaf to root paths  " + PriodeadlineleafToRootPaths );
		
		List<List<Integer>> PrioleafToRootPaths =new ArrayList<>();
		for(List<Integer> y : PriodeadlineleafToRootPaths.keySet())
			PrioleafToRootPaths.add(y);			
		//System.out.println("latencymoduleplacementnear prioritized leaf to root paths  " + PrioleafToRootPaths );	
		//System.out.println("===============================================================");
		
		for(List<Integer> path : PrioleafToRootPaths){
			placeModulesInPath(path);
			/*for(int i: path) {
			System.out.print(CloudSim.getEntityName(i));				
			System.out.println(" modulesload "+ getCurrentModuleLoadMap().get(i));
			System.out.println("moduleinstances " + getCurrentModuleInstanceNum().get(i));				
		}
		
		System.out.println("modulemap " + getCurrentModuleMap());
		System.out.println("modulesload "+ getCurrentModuleLoadMap());
		System.out.println("moduleinstances " + getCurrentModuleInstanceNum());
		System.out.println("=================================get next path");
		*/		
		}	
		
		/*for(int deviceId : getCurrentModuleMap().keySet()){ // device id has modules name list
			//System.out.println("latencymoduleplacementnear"+ getFogDeviceById(deviceId).getName()+ " CurrentModuleMap " + getCurrentModuleMap().get(deviceId));
			for(String module : getCurrentModuleMap().get(deviceId)){
				createModuleInstanceOnDevice(getApplication().getModuleByName(module), getFogDeviceById(deviceId));
			}
		}
		*/
		
		for(int deviceId : getCurrentModuleMapuser().keySet()){ // device id has modules name list
			//List<Pair<Integer,String>> LP = new ArrayList<Pair<Integer,String>>();			
			for(Pair<Integer,String> p : getCurrentModuleMapuser().get(deviceId)){				
				createModuleInstanceOnDevice(p.getKey(), getApplication().getModuleByName(p.getValue()), getFogDeviceById(deviceId));				
			}
			
			//System.out.println("latencymoduleplacementnear  CurrentModuleMapuser "+ getFogDeviceById(deviceId).getName()+ getCurrentModuleMapuser().get(deviceId));
			//System.out.println("latencymoduleplacementnear DeviceToModuleMapuser " +getFogDeviceById(deviceId).getName()+  getDeviceToModuleMapuser().get(deviceId));
		}
		//System.out.println("latencymoduleplacementnear  CurrentModuleMapuser "+ getCurrentModuleMapuser());
		//System.out.println("latencymoduleplacementnear DeviceToModuleMapuser " + getDeviceToModuleMapuser());
		
 /*
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
			int deviceId = CloudSim.getEntityId(deviceName);
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){				
				//getCurrentModuleLoadMap().get(deviceId).put(moduleName, getCurrentCpuLoad().get(deviceId));
				//System.out.println("Module Load Map at each device "+ getCurrentModuleLoadMap().get(deviceId));
				System.out.println(deviceName + moduleName+  " has mips " +getApplication().getModuleByName(moduleName).getMips()); 
			}			
		}
 */

	}

	/////////////////////////////////////////////////////////////////////////// ragaa  
		 
	private Double deadlineInfo(Integer mid) {
	
	List<List<Integer>> leafToRootPaths = getLeafToRootPaths();
	Map<Integer,Double> deadlines = new HashMap<>();
	for (List<Integer> x: leafToRootPaths) {
	Integer mobid= x.get(0);
	Double deadline = getApplication().getDeadlineInfo().get(mobid); 
	deadlines.put(mobid,deadline);
	}	
	return deadlines.get(mid);
	}
	
	////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////// ragaa  
		 
	private Double additionalmips(Integer mid) {
	
	List<List<Integer>> leafToRootPaths = getLeafToRootPaths();
	Map<Integer,Double> ecgguard = new HashMap<>();
	for (List<Integer> x: leafToRootPaths) {
	Integer mobid= x.get(0);
	Double additionalmips = getApplication().getAdditionalMipsInfo().get(mobid); 
	ecgguard.put(mobid,additionalmips);
	}
	return ecgguard.get(mid);
	}
	
////////////////////////////////////////////////////////////////////////////////////	

	/**
	 * Get the list of modules that are ready to be placed 
	 * @param placedModules Modules that have already been placed in current path
	 * @return list of modules ready to be placed
	 */
	private List<String> getModulesToPlace(List<String> placedModules){
		Application app = getApplication();
		List<String> modulesToPlace_1 = new ArrayList<String>();
		List<String> modulesToPlace = new ArrayList<String>();
		for(AppModule module : app.getModules()){
			if(!placedModules.contains(module.getName()))
				modulesToPlace_1.add(module.getName());
		}
		//System.out.println("ragaa modules to place 1  " + modulesToPlace_1);
		/*
		 * Filtering based on whether modules (to be placed) lower in physical topology are already placed
		 */
		for(String moduleName : modulesToPlace_1){
			boolean toBePlaced = true;
			
			for(AppEdge edge : app.getEdges()){
				//CHECK IF OUTGOING DOWN EDGES ARE PLACED
				if(edge.getSource().equals(moduleName) && edge.getDirection()==Tuple.DOWN && !placedModules.contains(edge.getDestination()))
					toBePlaced = false;
				//CHECK IF INCOMING UP EDGES ARE PLACED
				if(edge.getDestination().equals(moduleName) && edge.getDirection()==Tuple.UP && !placedModules.contains(edge.getSource()))
					toBePlaced = false;
			}
			if(toBePlaced)
				modulesToPlace.add(moduleName);
		}

		//System.out.println("ragaa modules to placeeeee    " + modulesToPlace);
		return modulesToPlace;
	}
	
///////////////////////////////////////////////////////////////////////////
	protected double getRateOfSensor(String sensorType){
		for(Sensor sensor : getSensors()){
			if(sensor.getTupleType().equals(sensorType))
				return 1/sensor.getTransmitDistribution().getMeanInterTransmitTime();
		}
		return 0;
	}
///////////////////////////////////////////////////////////////////////////
	private void placeModulesInPath(List<Integer> path) {
		Integer userid= path.get(0);
		if(path.size()==0)return;
		List<String> placedModules = new ArrayList<String>();
		Map<AppEdge, Double> appEdgeToRate = new HashMap<AppEdge, Double>();		
		
		/**
		 * Periodic edges have a fixed periodicity of tuples, so setting the tuple rate beforehand
		 */
		for(AppEdge edge : getApplication().getEdges()){
			if(edge.isPeriodic()){
				appEdgeToRate.put(edge, 1/edge.getPeriodicity());				
			}
		}
		
		for(Integer deviceId : path){
			FogDevice device = getFogDeviceById(deviceId);
			Map<String, Integer> sensorsAssociated = getAssociatedSensors(device);
			Map<String, Integer> actuatorsAssociated = getAssociatedActuators(device);
			placedModules.addAll(sensorsAssociated.keySet()); // ADDING ALL SENSORS TO PLACED LIST
			placedModules.addAll(actuatorsAssociated.keySet()); // ADDING ALL ACTUATORS TO PLACED LIST

			/*
			 * Setting the rates of application edges emanating from sensors
			 */
			for(String sensor : sensorsAssociated.keySet()){
				for(AppEdge edge : getApplication().getEdges()){
					if(edge.getSource().equals(sensor)){
						appEdgeToRate.put(edge, sensorsAssociated.get(sensor)*getRateOfSensor(sensor));						
					}
				}
			}
						
			/*
			 * Updating the AppEdge rates for the entire application based on knowledge so far
			 */
			boolean changed = true;
			while(changed){		//Loop runs as long as some new information is added
				changed=false;
				Map<AppEdge, Double> rateMap = new HashMap<AppEdge, Double>(appEdgeToRate);
				for(AppEdge edge : rateMap.keySet()){
					AppModule destModule = getApplication().getModuleByName(edge.getDestination());
					if(destModule == null)continue;
					Map<Pair<String, String>, SelectivityModel> map = destModule.getSelectivityMap();
					for(Pair<String, String> pair : map.keySet()){  // ragaa Pair<modulename, appid> //tupletype,> 
						if(pair.getFirst().equals(edge.getTupleType())){
							double outputRate = appEdgeToRate.get(edge)*map.get(pair).getMeanRate(); // getting mean rate from SelectivityModel
							AppEdge outputEdge = getApplication().getEdgeMap().get(pair.getSecond());
							if(!appEdgeToRate.containsKey(outputEdge) || appEdgeToRate.get(outputEdge)!=outputRate){
								// if some new information is available
								changed = true;
							}
							appEdgeToRate.put(outputEdge, outputRate);														
						}
					}
				}
			}
			//System.out.println("ragaa application edge to rate "+ appEdgeToRate);
			/*
			 * Getting the list of modules ready to be placed on current device on path
			 */
			List<String> modulesToPlace = getModulesToPlace(placedModules);
			
			while(modulesToPlace.size() > 0){ // Loop runs until all modules in modulesToPlace are deployed in the path
				String moduleName = modulesToPlace.get(0);
				double totalCpuLoad = 0;
				//double usrpriority= deadlineInfo(path.get(0));
				//System.out.println("user:" + path.get(0) + "userpriority " +usrpriority);				
				//for(AppEdge edge : getApplication().getEdges()){
					// getApplication().getEdgePrty().put(edge, usrpriority);
				//}
				//System.out.println("edgepriority :" +  getApplication().getEdgePrty());
				
				//double guardmips= additionalmips(path.get(0));
				//if (moduleName =="Client" || moduleName =="Caregiver" || moduleName == "Cloud_Analytics")	guardmips = 0.0;
				//System.out.println("Additional mips for module" +  moduleName + " is "+ guardmips );
				Double guardmips= additionalmips(path.get(0));	 
				
				//IF MODULE IS ALREADY PLACED UPSTREAM, THEN UPDATE THE EXISTING MODULE
				int upsteamDeviceId = isPlacedUpstream(moduleName, path);
				if(upsteamDeviceId > 0){  //already placed 
					if(upsteamDeviceId==deviceId){  // placed on my device 
						placedModules.add(moduleName);
						modulesToPlace = getModulesToPlace(placedModules);	
						
						// NOW THE MODULE TO PLACE IS IN THE CURRENT DEVICE. CHECK IF THE NODE CAN SUSTAIN THE MODULE
						//boolean outofrate = false;
						for(AppEdge edge : getApplication().getEdges()){		// take all incoming edges
							double rate = appEdgeToRate.get(edge); 
							if(edge.getDestination().equals(moduleName)){			
								totalCpuLoad += rate*(edge.getTupleCpuLength()+guardmips);								
							}
						}
						//if(device.getHost().getPower() > 327642.7372000124){
						if(totalCpuLoad + getCurrentCpuLoad().get(deviceId) > device.getHost().getTotalMips()){						
							//System.out.println("ragaa totalcpuload "+  totalCpuLoad +" currentcpuload" + getCurrentCpuLoad().get(deviceId)+ "on device " + device.getName()+" host totalmips "+device.getHost().getTotalMips());
							Logger.debug("latencyModulePlacement", "Need to shift module "+moduleName+" upstream from device " + device.getName());
							//System.out.println("Shifting extra instances of module "+moduleName+" upstream from device " + device.getName() + "totalCpuLoad "+ totalCpuLoad+  " modulesToPlace"+ modulesToPlace);
							//getCurrentModuleInstanceNum().get(deviceId).put(moduleName, getCurrentModuleInstanceNum().get(deviceId).get(moduleName));
							//getCurrentModuleLoadMap().get(deviceId).put(moduleName, totalCpuLoad* getCurrentModuleInstanceNum().get(deviceId).get(moduleName));
							//System.out.println("ragaa"+ getCurrentModuleInstanceNum().get(deviceId));
							//System.out.println("ragaa"+ getCurrentModuleLoadMap().get(deviceId));
							List<String> _placedOperators = shiftModuleNorth(moduleName, totalCpuLoad, deviceId, modulesToPlace, userid);
							//System.out.println("ragaa currentcpuload" + getCurrentCpuLoad().get(deviceId)+ "on device " + device.getName());
							for(String placedOperator : _placedOperators){
								if(!placedModules.contains(placedOperator))
									placedModules.add(placedOperator);
							}
							//System.out.println("ragaa"+ getCurrentModuleInstanceNum());
							//System.out.println("ragaa"+ getCurrentModuleLoadMap());
						}
						else{
							placedModules.add(moduleName);
							double x = getCurrentCpuLoad().get(deviceId) + totalCpuLoad;
							getCurrentCpuLoad().put(deviceId, x );
							//System.out.println(moduleName+ " "+ device.getName()+ "totalCpuLoad " +totalCpuLoad+  " getCurrentCpuLoad().get(deviceId)" + getCurrentCpuLoad().get(deviceId)+ " total " + device.getHost().getTotalMips());
							
							//ragaa add user to module placement/////////////// 
							Pair<Integer, String> p= new Pair<Integer, String>(userid, moduleName);
							getCurrentModuleMapuser().get(deviceId).add(p);
							//////////////////////////////////////////////////
							
							getCurrentModuleInstanceNum().get(deviceId).put(moduleName, getCurrentModuleInstanceNum().get(deviceId).get(moduleName)+1);
							getCurrentModuleLoadMap().get(deviceId).put(moduleName, totalCpuLoad+ getCurrentCpuLoad().get(deviceId));
							//getCurrentModuleLoadMap().get(deviceId).put(moduleName, totalCpuLoad*getCurrentModuleInstanceNum().get(deviceId).get(moduleName));//getCurrentCpuLoad().get(deviceId));
							Logger.debug("LatencyModulePlacement", "AppModule "+moduleName+" can be created on device "+device.getName());
							//System.out.println("ragaa now currentcpuload "+deviceId+ ": "+ getCurrentCpuLoad().get(deviceId));							
						}
					}
				}else{
					// FINDING OUT WHETHER PLACEMENT OF OPERATOR ON DEVICE IS POSSIBLE					
					for(AppEdge edge : getApplication().getEdges()){		// take all incoming edges
						double rate = appEdgeToRate.get(edge);						
						if(edge.getDestination().equals(moduleName)){							
							totalCpuLoad += rate*(edge.getTupleCpuLength()+ guardmips);							
								//System.out.println("ragaa me "+ moduleName + " tuplecpulength "+ edge.getTupleCpuLength() +" created on device "+device.getName()+ "total cpuload "+ totalCpuLoad);								
							}
					}
					//System.out.println(moduleName+ " "+ device.getName()+ "totalCpuLoad " +totalCpuLoad+  " getCurrentCpuLoad().get(deviceId)" + getCurrentCpuLoad().get(deviceId)+ " total " + device.getHost().getTotalMips());
					if(totalCpuLoad + getCurrentCpuLoad().get(deviceId) >  device.getHost().getTotalMips()){
						Logger.debug("ModulePlacementEdgeward", "Placement of operator "+moduleName+ " NOT POSSIBLE on device "+device.getName());
						System.out.println("Placement of operator "+moduleName+ " NOT POSSIBLE on device "+device.getName());
					}
					else{
						Logger.debug("ModulePlacementEdgeward", "Placement of operator "+moduleName+ " on device "+device.getName() + " successful. ");
						getCurrentCpuLoad().put(deviceId,  getCurrentCpuLoad().get(deviceId) + totalCpuLoad);
						
						if(!currentModuleMap.containsKey(deviceId)) {
							currentModuleMap.put(deviceId, new ArrayList<String>());
							currentModuleMap.get(deviceId).add(moduleName);							
						}
						else if(!currentModuleMap.get(deviceId).contains(moduleName))
							currentModuleMap.get(deviceId).add(moduleName);
						
						//ragaa code to add user
						Pair<Integer, String> p= new Pair<Integer, String>(userid, moduleName);
						if(!currentModuleMapuser.containsKey(deviceId)) {						 
							currentModuleMapuser.put(deviceId, new ArrayList<Pair<Integer, String>>());							
							currentModuleMapuser.get(deviceId).add(p);
						}
						else if(!currentModuleMapuser.get(deviceId).contains(p)) {								
								currentModuleMapuser.get(deviceId).add(p);
						}
						////////////
						placedModules.add(moduleName);
						modulesToPlace = getModulesToPlace(placedModules);
						getCurrentModuleLoadMap().get(deviceId).put(moduleName, totalCpuLoad);
						//System.out.println("ragaa now currentcpuload "+deviceId+" "+ moduleName+ ": "+ getCurrentCpuLoad().get(deviceId));
						//System.out.println("ragaa now getCurrentModuleLoadMap" + getCurrentModuleLoadMap().get(deviceId));
						int max = 1;
						for(AppEdge edge : getApplication().getEdges()){
							if(edge.getSource().equals(moduleName) && actuatorsAssociated.containsKey(edge.getDestination()))
								max = Math.max(actuatorsAssociated.get(edge.getDestination()), max);
							if(edge.getDestination().equals(moduleName) && sensorsAssociated.containsKey(edge.getSource()))
								max = Math.max(sensorsAssociated.get(edge.getSource()), max);
						}
						getCurrentModuleInstanceNum().get(deviceId).put(moduleName, max);
						//System.out.println("ragaa now getCurrentModuleInstanceNum "+ getCurrentModuleInstanceNum().get(deviceId));
					}
				}				
				modulesToPlace.remove(moduleName);
				//System.out.println("ragaa at end the final cpuload on device "+ device.getName() + " total load is " +getCurrentCpuLoad().get(deviceId));
			}
			//System.out.println("---------------------------------------");
		}
	}

	/*****************************************************************************************
	 * Shifts a module moduleName from device deviceId northwards. This involves other modules that depend on it to be shifted north as well.
	 * @param moduleName
	 * @param cpuLoad cpuLoad of the module
	 * @param deviceId
	 */
	private List<String> shiftModuleNorth(String moduleName, double cpuLoad, Integer deviceId, List<String> operatorsToPlace, Integer userid) {
		//System.out.println(CloudSim.getEntityName(deviceId)+" is shifting extra instances of module "+moduleName+" north.");
		//List<String> modulesToShift = findModulesToShift(moduleName, deviceId);		
		//Map<String, Integer> moduleToNumInstances = new HashMap<String, Integer>(); // Map of number of instances of modules that need to be shifted
		Map<String, Double> loadMap = new HashMap<String, Double>();		
		loadMap.put(moduleName, cpuLoad);	
		//System.out.println("ragaa after load" + loadMap.get(moduleName));
		//totalCpuLoad += cpuLoad;
		//System.out.println("ragaa totalcpuload "+totalCpuLoad);
		
		int id = getParentDevice(deviceId);
		//System.out.println("ragaa parentid "+ id +" CurrentCpuLoad "+ getCurrentCpuLoad().get(id));
		while(true){ // Loop iterates over all devices in path upstream from current device. Tries to place modules (to be shifted northwards) on each of them.
			if(id==-1){
				// Loop has reached the apex fog device in hierarchy, and still could not place modules. 
				Logger.debug("latencyModulePlacement", "Could not place module "+ moduleName+" northwards.");
				break;
			}
			FogDevice fogDevice = getFogDeviceById(id);
			//System.out.println("ragaa parent load "+ getCurrentCpuLoad().get(id)+ " totalcpuload toadd "+ totalCpuLoad);
			if(getCurrentCpuLoad().get(id) + cpuLoad > fogDevice.getHost().getTotalMips()){
				// Device cannot take up CPU load of incoming modules. Keep searching for device further north.
				id = getParentDevice(id); // iterating to parent device

			}else{ 
				// Device (@ id) can accommodate modules. Placing them here.
				double totalLoad = 0;
				for(String module : loadMap.keySet()){
					totalLoad += loadMap.get(module);
					if(!getCurrentModuleMap().get(id).contains(module)) {
						getCurrentModuleMap().get(id).add(module);
						getCurrentModuleLoadMap().get(id).put(module, loadMap.get(module));						
					}
					else {
						//getCurrentModuleMap().get(id).add(module);
						getCurrentModuleLoadMap().get(id).put(module, getCurrentModuleLoadMap().get(id).get(module)+loadMap.get(module));						
					}
					
					///ragaa code to add users
					Pair<Integer, String> p= new Pair<Integer, String>(userid, moduleName);
					if(!getCurrentModuleMapuser().get(id).contains(p)) {
						getCurrentModuleMapuser().get(id).add(p);												
					}
					///////////////////////////
					String module_ = module;
					int initialNumInstances = 0;
					if(getCurrentModuleInstanceNum().get(id).containsKey(module_))
						initialNumInstances = getCurrentModuleInstanceNum().get(id).get(module_);
		// int finalNumInstances = initialNumInstances + moduleToNumInstances.get(module_); 
					int finalNumInstances = ++initialNumInstances;
					getCurrentModuleInstanceNum().get(id).put(module_, finalNumInstances);
					//System.out.println("currentmodulemap " + getCurrentModuleMap());
					//System.out.println("currentmoduleloadmap " + getCurrentModuleLoadMap());
					//System.out.println("currentmoduleinistances " + getCurrentModuleInstanceNum());
				}
				
				getCurrentCpuLoad().put(id, getCurrentCpuLoad().get(id)+totalLoad);
				//System.out.println("deviceid " + id + " after shifting modules the current cpuload is  "+ getCurrentCpuLoad().get(id));
				operatorsToPlace.removeAll(loadMap.keySet());
				List<String> placedOperators = new ArrayList<String>();
				for(String op : loadMap.keySet())placedOperators.add(op);
				return placedOperators;
			}
		}		
		return new ArrayList<String>();
	}
//////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Get all modules that need to be shifted northwards along with <b>module</b>.  
	 * Typically, these other modules are those that are hosted on device with ID <b>deviceId</b> and lie upstream of <b>module</b> in application model. 
	 * @param module the module that needs to be shifted northwards
	 * @param deviceId the fog device ID that it is currently on
	 * @return list of all modules that need to be shifted north along with <b>module</b>
	 */
	private List<String> findModulesToShift(String module, Integer deviceId) {
		List<String> modules = new ArrayList<String>();
		modules.add(module);
		return findModulesToShift(modules, deviceId);
		/*List<String> upstreamModules = new ArrayList<String>();
		upstreamModules.add(module);
		boolean changed = true;
		while(changed){ // Keep loop running as long as new information is added.
			changed = false;
			for(AppEdge edge : getApplication().getEdges()){
				
				 * If there is an application edge UP from the module to be shifted to another module in the same device
				 
				if(upstreamModules.contains(edge.getSource()) && edge.getDirection()==Tuple.UP && 
						getCurrentModuleMap().get(deviceId).contains(edge.getDestination()) 
						&& !upstreamModules.contains(edge.getDestination())){
					upstreamModules.add(edge.getDestination());
					changed = true;
				}
			}
		}
		return upstreamModules;	*/
	}
	/**
	 * Get all modules that need to be shifted northwards along with <b>modules</b>.  
	 * Typically, these other modules are those that are hosted on device with ID <b>deviceId</b> and lie upstream of modules in <b>modules</b> in application model. 
	 * @param module the module that needs to be shifted northwards
	 * @param deviceId the fog device ID that it is currently on
	 * @return list of all modules that need to be shifted north along with <b>modules</b>
	 */
	private List<String> findModulesToShift(List<String> modules, Integer deviceId) {
		List<String> upstreamModules = new ArrayList<String>();
		upstreamModules.addAll(modules);
		boolean changed = true;
		while(changed){ // Keep loop running as long as new information is added.
			changed = false;
			/*
			 * If there is an application edge UP from the module to be shifted to another module in the same device
			 */
			for(AppEdge edge : getApplication().getEdges()){
				if(upstreamModules.contains(edge.getSource()) && edge.getDirection()==Tuple.UP && 
						getCurrentModuleMap().get(deviceId).contains(edge.getDestination()) 
						&& !upstreamModules.contains(edge.getDestination())){
					upstreamModules.add(edge.getDestination());
					changed = true;
				}
			}
		}
		return upstreamModules;	
	}
	
	private int isPlacedUpstream(String operatorName, List<Integer> path) {
		for(int deviceId : path){
			if(currentModuleMap.containsKey(deviceId) && currentModuleMap.get(deviceId).contains(operatorName))
				return deviceId;
		}
		return -1;
	}

	/**
	 * Gets all sensors associated with fog-device <b>device</b>
	 * @param device
	 * @return map from sensor type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedSensors(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for(Sensor sensor : getSensors()){
			if(sensor.getGatewayDeviceId()==device.getId()){
				if(!endpoints.containsKey(sensor.getTupleType()))
					endpoints.put(sensor.getTupleType(), 0);
				endpoints.put(sensor.getTupleType(), endpoints.get(sensor.getTupleType())+1);
			}
		}
		return endpoints;
	}
	
	/**
	 * Gets all actuators associated with fog-device <b>device</b>
	 * @param device
	 * @return map from actuator type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedActuators(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for(Actuator actuator : getActuators()){
			if(actuator.getGatewayDeviceId()==device.getId()){
				if(!endpoints.containsKey(actuator.getActuatorType()))
					endpoints.put(actuator.getActuatorType(), 0);
				endpoints.put(actuator.getActuatorType(), endpoints.get(actuator.getActuatorType())+1);
			}
		}
		return endpoints;
	}
	
	@SuppressWarnings("serial")
	protected List<List<Integer>> getPaths(final int fogDeviceId){
		FogDevice device = (FogDevice)CloudSim.getEntity(fogDeviceId); 
		if(device.getChildrenIds().size() == 0){		
			final List<Integer> path =  (new ArrayList<Integer>(){{add(fogDeviceId);}});
			List<List<Integer>> paths = (new ArrayList<List<Integer>>(){{add(path);}});
			return paths;
		}
		List<List<Integer>> paths = new ArrayList<List<Integer>>();
		for(int childId : device.getChildrenIds()){
			List<List<Integer>> childPaths = getPaths(childId);
			for(List<Integer> childPath : childPaths)
				childPath.add(fogDeviceId);
			paths.addAll(childPaths);
		}
		return paths;
	}
	
	protected List<List<Integer>> getLeafToRootPaths(){
		FogDevice cloud=null;
		for(FogDevice device : getFogDevices()){
			if(device.getName().equals("cloud"))
				cloud = device;
		}
		return getPaths(cloud.getId());
	}
	
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}

	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}

	public Map<Integer, List<String>> getCurrentModuleMap() {
		return currentModuleMap;
	}

	public void setCurrentModuleMap(Map<Integer, List<String>> currentModuleMap) {
		this.currentModuleMap = currentModuleMap;
	}
	/////////////////ragaa add user to CurrentModuleMap
	public Map<Integer, List<Pair<Integer,String>>> getCurrentModuleMapuser() {
		return currentModuleMapuser;
	}
	public void setCurrentModuleMapuser(Map<Integer, List<Pair<Integer,String>>> currentModuleMapuser) {
		this.currentModuleMapuser = currentModuleMapuser;
	}
	//	/////////////////////ragaa
	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<Integer, Double> getCurrentCpuLoad() {
		return currentCpuLoad;
	}

	public void setCurrentCpuLoad(Map<Integer, Double> currentCpuLoad) {
		this.currentCpuLoad= currentCpuLoad;
	}

	public Map<Integer, Map<String, Double>> getCurrentModuleLoadMap() {
		return currentModuleLoadMap;
	}

	public void setCurrentModuleLoadMap(
			Map<Integer, Map<String, Double>> currentModuleLoadMap) {
		this.currentModuleLoadMap = currentModuleLoadMap;
	}

	public Map<Integer, Map<String, Integer>> getCurrentModuleInstanceNum() {
		return currentModuleInstanceNum;
	}

	public void setCurrentModuleInstanceNum(
			Map<Integer, Map<String, Integer>> currentModuleInstanceNum) {
		this.currentModuleInstanceNum = currentModuleInstanceNum;
	}
}
