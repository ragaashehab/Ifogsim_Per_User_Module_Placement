package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;

public abstract class ModulePlacement {
	
	
	public static int ONLY_CLOUD = 1;
	public static int EDGEWARDS = 2;
	public static int USER_MAPPING = 3;
	
	private List<FogDevice> fogDevices;
	private Application application;
	private Map<String, List<Integer>> moduleToDeviceMap;
	private Map<Integer, List<AppModule>> deviceToModuleMap;
	private Map<Integer, Map<String, Integer>> moduleInstanceCountMap;
	
	protected abstract void mapModules();
	
	protected boolean canBeCreated(FogDevice fogDevice, AppModule module){	
		return fogDevice.getVmAllocationPolicy().allocateHostForVm(module);
	}
	
	protected int getParentDevice(int fogDeviceId){
		return ((FogDevice)CloudSim.getEntity(fogDeviceId)).getParentId();
	}
	
	protected FogDevice getFogDeviceById(int fogDeviceId){
		return (FogDevice)CloudSim.getEntity(fogDeviceId);
	}
	
	protected boolean createModuleInstanceOnDevice(AppModule _module, final FogDevice device, int instanceCount){
		return false;
	}
	
///////////////////////////ragaanew add hybrid module placement per user per module /////////////////////////////////////////////////////////////////////////////

	protected boolean createModuleInstanceOnDeviceHybrid(int userid, AppModule _module, final FogDevice device) {
		// TODO Auto-generated method stub
		AppModule module = null;
		if(getModuleToDeviceMap().containsKey(_module.getName())) {		
				module = new AppModule(_module);		
		}
		else 
			module = _module;	
		//System.out.print("moduleplacement userid "+userid+ " "+module.getName()+" has initial mips " +module.getMips());

		if(canBeCreated(device, module)){
			//System.out.println("moduleplacement Creating for userid "+ userid+ " "+module.getName()+" on device "+device.getName()+ "moduleprty "+ module.getPrty()+"  modulemips " + module.getMips() );
			if(!getDeviceToModuleMap().containsKey(device.getId()))
				getDeviceToModuleMap().put(device.getId(), new ArrayList<AppModule>());
			if(!getDeviceToModuleMap().get(device.getId()).contains(module))
				getDeviceToModuleMap().get(device.getId()).add(module);
			//System.out.println(device.getName()+ " DeviceToModuleMap "+ getDeviceToModuleMap());
			
			if(!getDeviceToModuleMapuser().containsKey(device.getId()))
				getDeviceToModuleMapuser().put(device.getId(), new ArrayList<Pair<Integer,AppModule>>());
			Pair<Integer,AppModule> p = new Pair<Integer,AppModule>(userid, module);
			getDeviceToModuleMapuser().get(device.getId()).add(p);
			//System.out.println(device.getName()+ " DeviceToModuleMap "+ getDeviceToModuleMap());
			
			if(!getModuleToDeviceMap().containsKey(module.getName()))
				getModuleToDeviceMap().put(module.getName(), new ArrayList<Integer>());
			getModuleToDeviceMap().get(module.getName()).add(device.getId());
			
			//if(!getModuleToDeviceMapUser().containsKey(module.getName()))
				//getModuleToDeviceMapUser().put(module.getName(), new ArrayList<Integer>());
			//getModuleToDeviceMapUser().get(module.getName()).add(device.getId());
			
			System.out.println("getDeviceToModuleMap "+ getDeviceToModuleMap());
			System.out.println("getDeviceToModuleMapuser "+getDeviceToModuleMapuser());
			System.out.println("getModuleToDeviceMap "  +getModuleToDeviceMap());
			System.out.println("=======================================================================");
			
			return true;
		} else {
			System.err.println("Module "+module.getName()+" cannot be created on device "+device.getName());
			System.err.println("Terminating");
			return false;
		}	
	}
///////////////////////////ragaa add users create instances for a list of users /////////////////////////////////////////////////////////////////////////////
	Map<Integer, Double> additionalMipsInfo = new HashMap<Integer, Double>(); // mobile id need additional mips int
	protected boolean createModuleInstanceOnDevice(int userid, AppModule _module, final FogDevice device){
		AppModule module = null;		
		if(getModuleToDeviceMap().containsKey(_module.getName()))		
			module = new AppModule(_module);		
		else 
			module = _module;	
		module.setPrty(userid); //ragaanew assign userid to each module
//////////////////////////ragaanew add variable  module mips to each user//////////////////////////////////// 	
		additionalMipsInfo= application.getAdditionalMipsInfo();
		double x=0;
		for(int usr:additionalMipsInfo.keySet()) {
			if (usr==userid)
				x= additionalMipsInfo.get(usr);				
		}		
	//	if (application.getplacement().equals("Latency Differentiated Module Placement")) {	
			if(module.getName().equals("Filtering")||module.getName().equals("Monitoring")||module.getName().equals("Caregiver")) {
				module.setMips(x);
			}
			//System.out.println("moduleplacement create module prty "+ module.getName()+" user "+ module.getPrty() + " mips "+ module.getMips());
	//	}
		
		//System.out.print("moduleplacement userid "+userid+ " "+module.getName()+" has initial mips " +module.getMips());
		
////////////////////////// end ragaanew add variable  module mips to each user//////////////////////////////////// 
		if(canBeCreated(device, module)){
			//System.out.println("moduleplacement Creating for userid "+ userid+ " "+module.getName()+" on device "+device.getName()+ "moduleprty "+ module.getPrty()+"  modulemips " + module.getMips() );
			if(!getDeviceToModuleMap().containsKey(device.getId()))
				getDeviceToModuleMap().put(device.getId(), new ArrayList<AppModule>());
			getDeviceToModuleMap().get(device.getId()).add(module);
			//System.out.println(device.getName()+ " DeviceToModuleMap "+ getDeviceToModuleMap());
			
			if(!getDeviceToModuleMapuser().containsKey(device.getId()))
				getDeviceToModuleMapuser().put(device.getId(), new ArrayList<Pair<Integer,AppModule>>());
			Pair<Integer,AppModule> p = new Pair<Integer,AppModule>(userid, module);
			getDeviceToModuleMapuser().get(device.getId()).add(p);
			//System.out.println(device.getName()+ " DeviceToModuleMap "+ getDeviceToModuleMap());
			
			if(!getModuleToDeviceMap().containsKey(module.getName()))
				getModuleToDeviceMap().put(module.getName(), new ArrayList<Integer>());
			getModuleToDeviceMap().get(module.getName()).add(device.getId());
			return true;
		} else {
			System.err.println("Module "+module.getName()+" cannot be created on device "+device.getName());
			System.err.println("Terminating");
			return false;
		}
	}
////////////////////////////////////////////////////////////////
	protected boolean createModuleInstanceOnDevice(AppModule _module, final FogDevice device){
		AppModule module = null;		
		if(getModuleToDeviceMap().containsKey(_module.getName()))		
			module = new AppModule(_module);		
		else 
			module = _module;
		
		if(canBeCreated(device, module)){
			//System.out.println("moduleplacement Creating "+module.getName()+" on device "+device.getName()+ "\t");
			
			if(!getDeviceToModuleMap().containsKey(device.getId()))
				getDeviceToModuleMap().put(device.getId(), new ArrayList<AppModule>());
			getDeviceToModuleMap().get(device.getId()).add(module);
			//System.out.println(device.getName()+ " DeviceToModuleMap "+ getDeviceToModuleMap());
			
			
			if(!getModuleToDeviceMap().containsKey(module.getName()))
				getModuleToDeviceMap().put(module.getName(), new ArrayList<Integer>());
			getModuleToDeviceMap().get(module.getName()).add(device.getId());
			return true;
		} else {
			System.err.println("Module "+module.getName()+" cannot be created on device "+device.getName());
			System.err.println("Terminating");
			return false;
		}
	}
////////////////////////////////////////////////////////////////////////////////////////ragaa
/* protected boolean createModuleInstanceOnDevice(AppModule _module, final FogDevice device){
		AppModule module = null;
		//System.out.println("ragaa module to device map " + getModuleToDeviceMap());
		//System.out.println("ragaa device to  module map " + getDeviceToModuleMap());
		
		if(getModuleToDeviceMap().containsKey(_module.getName())) {		
		module = new AppModule(_module);
		//System.out.println("ragaa the module exist in the map  "+ module.getName() ); 	
		}
		
		else {
		module = _module;
		//System.out.println("ragaa the module is not exist in the map  "+ module.getName() ); 	
		}
		
		if(canBeCreated(device, module)){
		System.out.println("Creating "+module.getName()+" on device "+device.getName());
		
		if(!getDeviceToModuleMap().containsKey(device.getId()))
		getDeviceToModuleMap().put(device.getId(), new ArrayList<AppModule>());
		getDeviceToModuleMap().get(device.getId()).add(module);
		
		if(!getModuleToDeviceMap().containsKey(module.getName())) {
		getModuleToDeviceMap().put(module.getName(), new ArrayList<Integer>());
		getModuleToDeviceMap().get(module.getName()).add(device.getId());
		}			
		else if(!getModuleToDeviceMap().get(module.getName()).contains(device.getId())) {
		//System.out.println("module to device map devices: " + getModuleToDeviceMap().get(module.getName()));
		getModuleToDeviceMap().get(module.getName()).add(device.getId());
		}
		
		return true;
		} else {
		System.err.println("Module "+module.getName()+" cannot be created on device "+device.getName());
		System.err.println("Terminating");
		return false;
}
}
*/
///////////////////////////////////////////////////////////////////////////////////////ragaa
	
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	protected FogDevice getDeviceByName(String deviceName) {
		for(FogDevice dev : getFogDevices()){
			if(dev.getName().equals(deviceName))
				return dev;
		}
		return null;
	}
	
	protected FogDevice getDeviceById(int id){
		for(FogDevice dev : getFogDevices()){
			if(dev.getId() == id)
				return dev;
		}
		return null;
	}
	
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public Map<String, List<Integer>> getModuleToDeviceMap() {
		return moduleToDeviceMap;
	}

	public void setModuleToDeviceMap(Map<String, List<Integer>> moduleToDeviceMap) {
		this.moduleToDeviceMap = moduleToDeviceMap;
	}

	public Map<Integer, List<AppModule>> getDeviceToModuleMap() {
		return deviceToModuleMap;
	}

	public void setDeviceToModuleMap(Map<Integer, List<AppModule>> deviceToModuleMap) {
		this.deviceToModuleMap = deviceToModuleMap;
	}
	
	/////////////ragaa add users /	deviceid, list of pair <userid, module>
	private Map<Integer, List<Pair<Integer, AppModule>>> deviceToModuleMapuser;
	public Map<Integer, List<Pair<Integer,AppModule>>> getDeviceToModuleMapuser() {
		return deviceToModuleMapuser;
	}
	public void setDeviceToModuleMapuser(Map<Integer, List<Pair<Integer, AppModule>>> deviceToModuleMapuser){  
		this.deviceToModuleMapuser= deviceToModuleMapuser;
	}
////////////////////////////////
	public Map<Integer, Map<String, Integer>> getModuleInstanceCountMap() {
		return moduleInstanceCountMap;
	}

	public void setModuleInstanceCountMap(Map<Integer, Map<String, Integer>> moduleInstanceCountMap) {
		this.moduleInstanceCountMap = moduleInstanceCountMap;
	}

}
