package org.fog.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.placement.LatencyModulePlacementNear;
import org.fog.placement.ModulePlacement;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

public class FogDevice extends PowerDatacenter {
	protected Queue<Tuple> northTupleQueue;
	protected Queue<Pair<Tuple, Integer>> southTupleQueue;
	
	protected List<String> activeApplications;
	
	protected Map<String, Application> applicationMap;	
	protected Map<Integer, Double> childToLatencyMap;
 
	
	protected Map<Integer, Integer> cloudTrafficMap;
	
	protected double lockTime;
	
	/**	
	 * ID of the parent Fog Device
	 */
	protected int parentId;
	
	/**
	 * ID of the Controller
	 */
	protected int controllerId;
	/**
	 * IDs of the children Fog devices
	 */
	protected List<Integer> childrenIds;

	protected Map<Integer, List<String>> childToOperatorsMap;
	
	/**
	 * Flag denoting whether the link southwards from this FogDevice is busy
	 */
	protected boolean isSouthLinkBusy;
	
	/**
	 * Flag denoting whether the link northwards from this FogDevice is busy
	 */
	protected boolean isNorthLinkBusy;
	
	protected double uplinkBandwidth;
	protected double downlinkBandwidth;
	protected double uplinkLatency;
	protected List<Pair<Integer, Double>> associatedActuatorIds;
	
	protected double energyConsumption;
	protected double lastUtilizationUpdateTime;
	protected double lastUtilization;
	private int level;
	
	protected double ratePerMips;
	
	protected double totalCost;
	
	protected Map<String, Map<String, Integer>> moduleInstanceCount; //<"appid", <"modulename", count>>
	
////////////////////////////////////////ragaa 
	public Map<String, List<String>> appToModulesMap;  //changed ragaa
	// my var
	protected String placement;
	protected List<Integer> idOfEndDevices;	
	protected Map<Integer,FogDevice> deviceById;
	protected Map<Integer, Double> deadlineInfo;// = new HashMap<Integer, Double>();  // mobile id , priority
	protected Map<Integer, Double> additionalMipsInfo; // mobile id tuple mips int
	protected Map<Integer,List<Map<String, Double>>> deadlineInfomodule1; //ragaanew
	protected Map<Integer, Map<String, Double>> deadlineInfomodule;// = new HashMap<Integer, Map<String,Double>>();  // mobile id need deadline double
	protected Map <String,Double> moduledeadline; // = new HashMap<String,Double>(); //for specific device
	protected Map<AppModule, Double> Appmoduledeadline; //= new HashMap<AppModule,Double>()
	
	public Map<String, List<Pair<Integer,String>>> appToModulesMapUsers; //at me <appname ,list <idofenddevice, modulename>> users exist on my device 		
	//protected Map<Integer, List<Pair<Integer,String>>> currentModuleMapuser; //ragaa deviceid, list<pair<userid,modulename>>
	//protected Map<String,  List<Pair<Integer,String>>> appToModulesMapUsersDept; // <appname ,<idofenddevice, modulename>>
	public Map<Integer,List<String>> unsatisfiedUsers; //ragaanew
	public Map<Integer, List<AppModule>> unsatisfiedvms;//= new HashMap<Integer, List<String>>();
	public ModulePlacement mymoduleplacement; // to get device user modules from the placement class
	public Map<Double,Integer> unsatisfiedUserstimeline;	

	public Map<Double,Double> Cputilization;	 
	public List<Double> rem;   		// all new uti - previous uti
	public List<Double> recentrem;  // five recent remaining 
	boolean mig= true ; 
	boolean mig1= false ; boolean mig2= false ; boolean mig3= false ;
	//protected double utinew =0.0; protected double utiold =0.0;
	protected boolean Executing; 
	protected int finst =0;  // filtering instances
	protected int minst =0; //monitoring
	protected int cinst=0; //caregiver instances
	/////////////////////////////////////////////////
	
	public FogDevice(
			String name, 
			FogDeviceCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList,
			double schedulingInterval,
			double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setCharacteristics(characteristics);
		setVmAllocationPolicy(vmAllocationPolicy);
		setLastProcessTime(0.0);
		setStorageList(storageList);
		setVmList(new ArrayList<Vm>());
		setSchedulingInterval(schedulingInterval);
		setUplinkBandwidth(uplinkBandwidth);
		setDownlinkBandwidth(downlinkBandwidth);
		setUplinkLatency(uplinkLatency);
		setRatePerMips(ratePerMips);
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
		for (Host host : getCharacteristics().getHostList()) {
			host.setDatacenter(this);
		} 
		setActiveApplications(new ArrayList<String>());
		// If this resource doesn't have any PEs then no useful at all
		if (getCharacteristics().getNumberOfPes() == 0) {
			throw new Exception(super.getName()
					+ " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		}
		// stores id of this class
		getCharacteristics().setId(super.getId());
		
		applicationMap = new HashMap<String, Application>();
		appToModulesMap = new HashMap<String, List<String>>();
		
		appToModulesMapUsers= new HashMap<String, List<Pair<Integer,String>>>(); //ragaa appname, mobid,modulename
		//currentModuleMapuser= new HashMap<Integer, List<Pair<Integer,String>>>(); //ragaa deviceid, list<pair<userid,modulename>>
		deadlineInfo = new HashMap<Integer, Double>();  // mobile id need deadline double
		deadlineInfomodule = new HashMap<Integer, Map<String,Double>>();  // mobile id need deadline double
		moduledeadline = new HashMap<String,Double>(); //deadlineInfomodule for specific device
		Appmoduledeadline = new HashMap<AppModule,Double>();
		additionalMipsInfo = new HashMap<Integer, Double>(); // mobile id need additional mips int
		Executing=false;
		Cputilization= new HashMap<Double, Double>(); // device cpu utilization
		rem= new ArrayList<Double>();
		recentrem = new ArrayList<Double>();
		deadlineInfomodule1 = new HashMap<Integer, List<Map<String,Double>>>(); //ragaanew
		unsatisfiedUsers= new HashMap<Integer, List<String>>(); //Map<Integer,List<String>> unsatisfiedUsers;
		unsatisfiedvms = new HashMap<Integer, List<AppModule>>();
		//mymoduleplacement = new ModulePlacement ;
		unsatisfiedUserstimeline= new HashMap<Double, Integer>();	

		northTupleQueue = new LinkedList<Tuple>();
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		
		
		setChildrenIds(new ArrayList<Integer>());
		setChildToOperatorsMap(new HashMap<Integer, List<String>>());
		
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		
		this.lockTime = 0;
		
		this.energyConsumption = 0;
		this.lastUtilization = 0;
		setTotalCost(0);
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
		setChildToLatencyMap(new HashMap<Integer, Double>());	

	}

	public FogDevice(
			String name, long mips, int ram, 
			double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
		super(name, null, null, new LinkedList<Storage>(), 0);
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				powerModel
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));
		
		String arch = Config.FOG_DEVICE_ARCH; 
		String os = Config.FOG_DEVICE_OS; 
		String vmm = Config.FOG_DEVICE_VMM;
		double time_zone = Config.FOG_DEVICE_TIMEZONE;
		double cost = Config.FOG_DEVICE_COST; 
		double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
		double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
		double costPerBw = Config.FOG_DEVICE_COST_PER_BW;

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		setCharacteristics(characteristics);
		
		setLastProcessTime(0.0);
		setVmList(new ArrayList<Vm>());
		setUplinkBandwidth(uplinkBandwidth);
		setDownlinkBandwidth(downlinkBandwidth);
		setUplinkLatency(uplinkLatency);
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
		for (Host host1 : getCharacteristics().getHostList()) {
			host1.setDatacenter(this);
		}
		setActiveApplications(new ArrayList<String>());
		if (getCharacteristics().getNumberOfPes() == 0) {
			throw new Exception(super.getName()
					+ " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		}
		
		
		getCharacteristics().setId(super.getId());
		
		applicationMap = new HashMap<String, Application>();
		appToModulesMap = new HashMap<String, List<String>>();
		
		appToModulesMapUsers=  new HashMap<String, List<Pair<Integer,String>>>(); //ragaa appname, mobid,modulename
		//currentModuleMapuser= new HashMap<Integer, List<Pair<Integer,String>>>(); //ragaa deviceid, list<pair<userid,modulename>>
		deadlineInfo = new HashMap<Integer, Double>();  // mobile id need deadline double
		deadlineInfomodule = new HashMap<Integer, Map<String,Double>>();  // mobile id need deadline double
		moduledeadline = new HashMap<String,Double>(); //deadlineInfomodule for specific device
		Appmoduledeadline = new HashMap<AppModule,Double>();
		additionalMipsInfo = new HashMap<Integer, Double>(); // mobile id need additional mips int
		Executing=false;
		Cputilization= new HashMap<Double, Double>(); // device cpu utilization
		rem= new ArrayList<Double>();
		recentrem = new ArrayList<Double>();
		deadlineInfomodule1 = new HashMap<Integer, List<Map<String,Double>>>(); //ragaanew
		unsatisfiedUsers= new HashMap<Integer, List<String>>(); //Map<Integer,List<String>> unsatisfiedUsers;
		unsatisfiedvms = new HashMap<Integer, List<AppModule>>();
		unsatisfiedUserstimeline= new HashMap<Double, Integer>();	


		northTupleQueue = new LinkedList<Tuple>();
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		
		
		setChildrenIds(new ArrayList<Integer>());
		setChildToOperatorsMap(new HashMap<Integer, List<String>>());
		
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		
		this.lockTime = 0;
		
		this.energyConsumption = 0;
		this.lastUtilization = 0;
		setTotalCost(0);
		setChildToLatencyMap(new HashMap<Integer, Double>());
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
	}
	
	/**
	 * Overrides this method when making a new and different type of resource. <br>
	 * <b>NOTE:</b> You do not need to override {@link #body()} method, if you use this method.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void registerOtherEntity() {
		
	}
	
	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:			
			processTupleArrival(ev); 
			break;
		case FogEvents.LAUNCH_MODULE:
			processModuleArrival(ev);
			break;
		case FogEvents.RELEASE_OPERATOR:
			processOperatorRelease(ev);
			break;
		case FogEvents.SENSOR_JOINED:
			processSensorJoining(ev);
			break;
		case FogEvents.SEND_PERIODIC_TUPLE:
			sendPeriodicTuple(ev);
			break;
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
			updateNorthTupleQueue();
			break;
		case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
			updateSouthTupleQueue();
			break;
		case FogEvents.ACTIVE_APP_UPDATE:
			updateActiveApplications(ev);
			break;
		case FogEvents.ACTUATOR_JOINED:
			processActuatorJoined(ev);
			break;
		case FogEvents.LAUNCH_MODULE_INSTANCE:
			updateModuleInstanceCount(ev);
			break;
		case FogEvents.RESOURCE_MGMT:
			manageResources(ev);
		default:
			break;
		}
	}
	
	/**
	 * Perform miscellaneous resource management tasks
	 * @param ev
	 */
	private void manageResources(SimEvent ev) {
		updateEnergyConsumption();
		send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
				
		if(this.Cputilization.containsKey(CloudSim.clock()))
			this.Cputilization.put(CloudSim.clock()+0.0000000000001, this.getHost().getUtilizationOfCpu());
		else
			this.Cputilization.put(CloudSim.clock(), this.getHost().getUtilizationOfCpu());
		if(this.unsatisfiedUserstimeline.containsKey(CloudSim.clock()))
			this.unsatisfiedUserstimeline.put(CloudSim.clock()+0.0000000000001, this.unsatisfiedUsers.size());
		else
			this.unsatisfiedUserstimeline.put(CloudSim.clock(), this.unsatisfiedUsers.size());
		

	}

	/**
	 * Updating the number of modules of an application module on this device
	 * @param ev instance of SimEvent containing the module and no of instances 
	 */
	private void updateModuleInstanceCount(SimEvent ev) {		
		//System.out.println(getName()+ " Creating instances of module "+ ev.getData());
		Object[] data = (Object[]) ev.getData();
		AppModule mod= (AppModule)data[0];
		Integer idOfEndDevice = (int)data[1];		
		Integer inst= (int)data[2];
		
		//ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
		ModuleLaunchConfig config = new ModuleLaunchConfig(mod, inst);
		String appId = config.getModule().getAppId();
		if(!moduleInstanceCount.containsKey(appId))
			moduleInstanceCount.put(appId, new HashMap<String, Integer>());
		moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
		//System.out.println(getName()+ " Creating "+config.getInstanceCount()+" instances of module "+config.getModule().getName());
		
///////////ragaa code to increase no of inistance for each  user
		if(!this.appToModulesMapUsers.containsKey(appId)){
			this.appToModulesMapUsers.put(appId,  new ArrayList<Pair<Integer,String>>());
			}
		
		if(this.placement.equals("Module Placement Edgeward")&& !(this.getName().startsWith("m"))) {
			//System.out.println(" updateModuleInstanceCount "+this.getName()+  mod.getName() + "inst "+ inst + "  "+idOfEndDevice);
			for(int i=0; i<inst; i++) {
				Pair<Integer,String> n = new Pair<Integer,String>(++idOfEndDevice,mod.getName());
				this.appToModulesMapUsers.get("ECG").add(n);
			}			
		}
		if(this.placement.equals("Module Placement Edgeward")&& this.getName().startsWith("m")) {
			//System.out.println(" updateModuleInstanceCount "+this.getName()+  mod.getName() + "inst "+ inst + "  "+idOfEndDevice);
			for(int i=0; i<inst; i++) {
				Pair<Integer,String> n = new Pair<Integer,String>(idOfEndDevice,mod.getName());
				if (!this.appToModulesMapUsers.get("ECG").contains(n))
					this.appToModulesMapUsers.get("ECG").add(n);
			}			
		}
		if(!this.placement.equals("Module Placement Edgeward")) 
		{
			//System.out.println("updateModuleInstanceCount " + this.getName()+ " befor appToModulesMapUsers "+ this.appToModulesMapUsers);
			Pair<Integer,String> n = new Pair<Integer,String>(idOfEndDevice,mod.getName());
			this.appToModulesMapUsers.get("ECG").add(n);
			//System.out.println("updateModuleInstanceCount " + this.getName()+ " after appToModulesMapUsers "+ this.appToModulesMapUsers);	
			List<Pair<Integer, String>> dumappToModulesMapUsers= new ArrayList<Pair<Integer, String>>();
			Pair<Integer, String> ddumappToModulesMapUsers;
			for(int id: this.deadlineInfo.keySet()) {			
				for(Pair<Integer,String> p: this.appToModulesMapUsers.get("ECG")){
					if(id==p.getKey()) {					
						ddumappToModulesMapUsers= new Pair<Integer, String>(p.getKey(), p.getValue());
						dumappToModulesMapUsers.add(ddumappToModulesMapUsers);				
					}
				}
			}			
			Map<String, List<Pair<Integer, String>>> ASCappToModulesMapUsers= new HashMap<String, List<Pair<Integer, String>>>();
			ASCappToModulesMapUsers.put("ECG", dumappToModulesMapUsers);
			this.appToModulesMapUsers= ASCappToModulesMapUsers;
		}
		//System.out.println(" hiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii=============================================");
		//System.out.println("updateModuleInstanceCount " + this.getName()+ " ASC sort of  appToModulesMapUsers "+ this.appToModulesMapUsers);
	}
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private AppModule getModuleByName(String moduleName){
		AppModule module = null; // canceled by ragaa
		module = applicationMap.get("ECG").getModuleByName(moduleName); //ragaa 		
		for(Vm vm : getHost().getVmList()){
			//System.out.println("fogdevice getModuleByName "+ this.getName()+ "  "+ ((AppModule)vm).getName()); 
			if(((AppModule)vm).getName().equals(moduleName)){				
				module=(AppModule)vm;
				break;
			}
		}
		return module;
	}
	
	/**
	 * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, only one tuple is sent DOWN while instanceCount number of tuples are sent UP.
	 * @param ev SimEvent instance containing the edge to send tuple on
	 */
	private void sendPeriodicTuple(SimEvent ev) {
		AppEdge edge = (AppEdge)ev.getData();
		String srcModule = edge.getSource();
		AppModule module = getModuleByName(srcModule);
		
		if(module == null)
			return;
		
		int instanceCount = module.getNumInstances();
		/*
		 * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
		 */
		for(int i = 0;i<((edge.getDirection()==Tuple.UP)?instanceCount:1);i++){
			//System.out.println(CloudSim.clock()+" : Sending periodic tuple "+edge.getTupleType());
			Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId(), module.getId());
			updateTimingsOnSending(tuple);
			sendToSelf(tuple);			
		}
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}

	protected void processActuatorJoined(SimEvent ev) {
		int actuatorId = ev.getSource();
		double delay = (double)ev.getData();
		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
	}

	
	protected void updateActiveApplications(SimEvent ev) {
		//Application app = (Application)ev.getData();
		//ragaanew recieve moduleplacement form controller
		Object[] data = (Object[]) ev.getData();
		Application app = (Application)data[0];
		ModulePlacement modulePlacement =(ModulePlacement)data[1]; 
		getActiveApplications().add(app.getAppId());
		mymoduleplacement = modulePlacement;
	}

	
	public String getOperatorName(int vmId){
		for(Vm vm : this.getHost().getVmList()){
			if(vm.getId() == vmId)
				return ((AppModule)vm).getName();
		}
		return null;
	}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;

		for (PowerHost host : this.<PowerHost> getHostList()) {
			Log.printLine();
			//Log.printLine("start fogdevice call updatevmprocessing host "+ host.getId());
						
			for(Vm vm: this.getHost().getVmList()) {
				 vm.setBeingInstantiated(true);  // (very tricky) ragaa so all vms got total allocatedmips no need to updatevmprocessing
				 //System.out.println("id " + this.getId()+" vmid "+ ((AppModule)vm).getName()+ "  total allocated mips "+this.getHost().getAllocatedMipsForVm(vm));
				//List<Double>reqmips= this.getHost().getAllocatedMipsForVm(vm);
				// reqmips.add(this.additionalMipsInfo.get(mobileid)));
				//vm.getCloudletScheduler().updateVmProcessing(currentTime, reqmips);
			} 

			/*
//ragaa////////////code to assign the requested additional mips for each user's tuples /////////////////
		  if(!this.placement.equals("Module Placement Edgeward")) {
			List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");
			for(Vm vm: this.getHost().getVmList()) {
				String nam= ((AppModule)vm).getName();				
					for(Pair<Integer, String> y: pp) {
						if (y.getValue().equals(nam)) {
							List<Double>reqmips= vm.getCloudletScheduler().getCurrentRequestedMips();
							//System.out.println("fogdevice user "+y.getKey()+ ", " +((AppModule)vm).getName() + " requested mips "+ reqmips);
							reqmips.clear();
							for(int a: this.additionalMipsInfo.keySet()) {
								if (a==y.getKey()) {
									double x= this.additionalMipsInfo.get(a);
									if (nam.equals("Cloud_Analytics"))
										reqmips.add(x+1000.00); // cloud analytics has 1000 mips extra over additional mips 
									else 
										reqmips.add(x);
									vm.getCloudletScheduler().updateVmProcessing(currentTime, reqmips);
									reqmips= vm.getCloudletScheduler().getCurrentRequestedMips();
									//System.out.println("fogdevice user "+y.getKey()+ ", " +((AppModule)vm).getName() + " new requested mips "+ reqmips);
			}}}}}}
		  
		  else { // placement edgeward
			//System.out.println("================================================================");
			//System.out.println(this.getName()+ " add additional mips to " + placement);   
			//if(!this.getName().startsWith("m")) {
				//List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");
				//System.out.println(this.getName()+ " appToModulesMapUsers pp "+ pp);
				for(Vm vm: this.getHost().getVmList()) {
					String nam= ((AppModule)vm).getName();
					//System.out.println(this.getName()+ " vm id "+ vm.getId()+ " "+ nam + " vmlistsize "+this.getHost().getVmList().size());
						//for(Pair<Integer, String> y: pp) {
							//if (y.getValue().equals(nam)) {
								//List<Double>reqmips= vm.getCloudletScheduler().getCurrentRequestedMips();
					List<Double>reqmips= new ArrayList<Double>();
								//System.out.println("fogdevice edgeward user befor "+y.getKey()+ ", " +((AppModule)vm).getName() + " requested mips "+ reqmips);
								//reqmips.clear();
					Map.Entry<Integer, Double> entry = this.additionalMipsInfo.entrySet().iterator().next();
					//System.out.println("additionalMipsInfo entry "+ entry );
					double x= entry.getValue();
								
					if (nam.equals("Cloud_Analytics"))
						reqmips.add(x+1000.00); // cloud analytics has 1000 mips extra over additional mips 
					else 
						reqmips.add(x);
					//this.getHost().getVmScheduler().deallocatePesForAllVms();
					//this.getHost().getVmScheduler().allocatePesForVm(vm, reqmips);								
							
					vm.getCloudletScheduler().updateVmProcessing(currentTime, reqmips);										
//					List<Double> readreqmips= vm.getCloudletScheduler().getCurrentRequestedMips();
								
					//System.out.println(this.getName()+ ((AppModule)vm).getName() + vm.getId()+ " requested cloudlet processing mips "+ vm.getCloudletScheduler().getCurrentRequestedMips() + " vm allocatedmips "+ vm.getCurrentAllocatedMips());											
					//reqmips.clear();
								//}
							//}
						}
				}                  //}}

			  else // edgeward , mobile device 
			  {	  System.out.println(this.getName()+ " add additional mips to " + placement);
				  List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");
					for(Vm vm: this.getHost().getVmList()) {
						String nam= ((AppModule)vm).getName();				
							for(Pair<Integer, String> y: pp) {
								if (y.getValue().equals(nam)) {
									List<Double>reqmips= vm.getCloudletScheduler().getCurrentRequestedMips();
									//System.out.println("fogdevice edgeward user "+y.getKey()+ ", " +((AppModule)vm).getName() + " requested mips "+ reqmips);
									reqmips.clear();
									for(int a: this.additionalMipsInfo.keySet()) {
										if (a==y.getKey()) {
											double x= this.additionalMipsInfo.get(a);
											if (nam.equals("Cloud_Analytics"))
												reqmips.add(x+1000.00); // cloud analytics has 1000 mips extra over additional mips 
											else 
												reqmips.add(x);
											vm.getCloudletScheduler().updateVmProcessing(currentTime, reqmips);
											reqmips= vm.getCloudletScheduler().getCurrentRequestedMips();
											//System.out.println("fogdevice edgeward user "+y.getKey()+ ", " +((AppModule)vm).getName() + " new requested mips "+ reqmips);
					}}}}} 
			  }
			
			} 
			
			*/
			
//////////////////////////////////end code to assign additional mips for each user//////////////////////////
			
			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing			
			
			if (time < minTime) {minTime = time;}
		
			Log.formatLine("%.2f: [Host #%d] utilization is %.2f%%",currentTime,host.getId(),	host.getUtilizationOfCpu() * 100);
			//System.out.println(this.getName()+" utilization "+	host.getUtilizationOfCpu() * 100);
////////////////////////ragaa store all utilization readings//////////////////
			//if(this.Cputilization.containsKey(CloudSim.clock()))
				//this.Cputilization.put(CloudSim.clock()+0.0000000000001, host.getUtilizationOfCpu());
			//else
				//this.Cputilization.put(CloudSim.clock(), host.getUtilizationOfCpu());
			//if(this.getName().startsWith("d"))
			//System.out.println(this.getName()+" CPUtilization "+ this.Cputilization);
		
////////////////// ragaa store difference between each two utilizations//////////////////////
			/*
			utinew= host.getUtilizationOfCpu(); utiold=host.getPreviousUtilizationOfCpu();
			double remvar = host.getUtilizationOfCpu()*100-host.getPreviousUtilizationOfCpu()*100;       // utinew - utiold;  // //TimeUnit.SECONDS.sleep(10);
			this.rem.add(remvar);		
			
			
			//System.out.println(this.getName()+" remaining utilization "+ this.rem);
						
			recentrem.clear();
			Collections.reverse(this.rem);			
			for(int z=0; z<5; z++) {
				if (z< this.rem.size()) {
					recentrem.add(this.rem.get(z));
					}
				}
			Collections.reverse(this.rem);					
			//System.out.println(this.getName()+" last 5 recent remaining utilizations  "+ recentrem);
			*/
				
		} // end for host	
///////////////////////////////start calculate power //////////////////////////////
		if (timeDiff > 0) {
			Log.formatLine("\nEnergy consumption for the last time frame from %.2f to %.2f:",getLastProcessTime(),currentTime);
			//System.out.println(this.getName()+ " Energy consumption for the last time frame from "+getLastProcessTime()+ " to " +currentTime);
			
			for (PowerHost host : this.<PowerHost> getHostList()) {				
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				//if(this.getName()=="cloud") {					
					//System.out.println("error here ======="+this.getName()+ " previous "+ previousUtilizationOfCpu+ " utilization "+ utilizationOfCpu+"  time  "+timeDiff);
					//previousUtilizationOfCpu=0.0;
				//}
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
						previousUtilizationOfCpu,
						utilizationOfCpu,
						timeDiff);
				
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				Log.printLine();
				Log.formatLine("%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",currentTime,host.getId(),
						getLastProcessTime(),previousUtilizationOfCpu * 100,utilizationOfCpu * 100);
				//System.out.println(this.getName() + " utilization at "+getLastProcessTime()+" was "+ previousUtilizationOfCpu * 100+ ", now is "+utilizationOfCpu * 100);
				Log.formatLine("%.2f: [Host #%d] energy is %.2f W*sec",	currentTime,host.getId(),timeFrameHostEnergy);
				//System.out.println(this.getName() +  " host energy is "+ timeFrameHostEnergy+ " W*sec");
			}

			Log.formatLine("\n%.2f: Data center's energy is %.2f W*sec\n",currentTime,timeFrameDatacenterEnergy);
			//System.out.println(this.getName() + " Data center's energy is "+ timeFrameDatacenterEnergy+ " W*sec");
		} // end if  timediff>0
		
		setPower(getPower() + timeFrameDatacenterEnergy);
		
		checkCloudletCompletion();		
		
/////////////////////////////////////ragaa/////////////////////////////////
///////////////// without utilization enhancement ///////////////////////////////////
		/*
		 if(this.placement.equals("Module Placement Edgeward"))
			checkDevicePerformanceEdgeward();
		else
			checkDevicePerformance();
		

///////////////with utilization enhancement///////////////////////////////////
		/*
		for(double x: this.Cputilization.keySet()) {				
			this.rem.add(this.Cputilization.get(x));
		}
		this.recentrem.clear();
		Collections.reverse(this.rem);			
		for(int z=0; z< 10; z++) {
			if (z< this.rem.size()) {
				recentrem.add(this.rem.get(z));
				}
			}
		Collections.reverse(this.rem);
		
		if(this.recentrem.size() == 10) {
			 this.mig1 = this.mig2 = this.mig3 = false;
			 double a=this.recentrem.get(0); double b=this.recentrem.get(1); double c=this.recentrem.get(2);double d=this.recentrem.get(3);double e=this.recentrem.get(4); double f=this.recentrem.get(5); double g=this.recentrem.get(6);double h =this.recentrem.get(7);double j=this.recentrem.get(8);double k =this.recentrem.get(9);
			 //if(a>0 && b>0 && c>0)  mig = true; // increase					
			 //if(a<0 && b<0 && c<0) mig=false; // decrease				 
			 //if(a==0 &&b==0 &&c==0) 	mig=false; // const
			 //if(a>0 &&b==0 &&c<0)  mig=false;
			 //if(a>=90 && b>=90 && c>=90 && d>=90 && e>=90)  mig = true; // increase
			 if(a==b && b==c && c==d )  this.mig1 = true;  //3 points
			 if(a==b && b==c && c==d && d==e && e==f && f==g)  this.mig2 = true; // 7 points
			 if(a==b && b==c && c==d && d==e && e==f && f==g && g==h&& h==j&& j==k)  this.mig3 = true; // 10 points
			 this.recentrem.clear();
		}
		
		if(this.getHost().getUtilizationOfCpu() > 1){
			//System.out.println(this.getName()+ "\t "+ this.getHost().getUtilizationOfCpu()+ "\t mig1 "+this.mig1+ "\t mig2 "+this.mig2);
			if(this.placement.equals("Module Placement Edgeward")) {
				if(this.mig2==true) {
					System.out.println(this.getName()+ " migrate under enhanced condition 1");
					checkDevicePerformanceEdgewardEnhanced();
				}
			}
			else {
				if(this.mig1==true)
				checkDevicePerformanceEnhanced();				
			}			
		}
		if (!this.getName().startsWith("m")) { 
			if((this.getHost().getUtilizationOfCpu()>=0.999) && (this.getHost().getUtilizationOfCpu()<= 1 )) {				
				//System.out.println(this.getName()+ "\t "+ this.getHost().getUtilizationOfCpu()+ "\t mig1 "+this.mig1+ "\t mig2 "+this.mig2);
				if(this.placement.equals("Module Placement Edgeward")) {					
					if(this.mig3==true) {
						//System.out.println(this.getName()+ " migrate under enhanced condition 2");	
						checkDevicePerformanceEdgewardEnhanced();
					}
				}
				else {
					if(this.mig2==true)
					checkDevicePerformanceEnhanced();					
					}
				}
			}
		else {  // mobile migration condition  
			if((this.getHost().getUtilizationOfCpu()>=0.3) && (this.getHost().getUtilizationOfCpu()<=1)) {
				if(this.placement.equals("Module Placement Edgeward")) {					
					if(this.mig3==true) 
					checkDevicePerformanceEdgewardEnhanced();
				}
				else {
					if(this.mig2==true)
					checkDevicePerformanceEnhanced();
					}
				}
			}
			/*
////////////////////////////////////ragaa/////////////////////////////////		
		// commented all checkdeviceperformanceenhanced 
//////////////////////////////////////ragaa/////////////////////////////////end
 */
///////////////////////////////////////ragaanew check  user delays//////////////////////////////////////////////
///////////////////////////////////////ragaanew check if average delay loop analytics user exceed limit //////////////////////////////////////////////
		
if(this.placement.equals("Enhanced Latency Differentiated Module Placement")) {
	
		for(AppLoop loop :  this.getApplicationMap().get("ECG").getLoops()){
			//System.out.println("AverageUserAnalytics "+ TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().get(loop.getLoopId()));
			Map<Integer,Double> badusraveragedelay= new HashMap<Integer,Double>() ;
			badusraveragedelay= TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().get(loop.getLoopId());
			if(badusraveragedelay!=null) {
			if(badusraveragedelay.size()==this.idOfEndDevices.size()) {	
				for (Map.Entry<Integer,Double> entry : badusraveragedelay.entrySet()) {  
					if(entry.getValue()> 50 )//)
						this.unsatisfiedUsers.put(entry.getKey(), new ArrayList<String>());
					else if(this.unsatisfiedUsers.containsKey(entry.getKey()))
						this.unsatisfiedUsers.remove(entry.getKey());	
				}}}}
			
		
		Map<Integer,List< Map<String,Double>>> UsertupleTypeToAverageCpuTime= TimeKeeper.getInstance().getUsertupleTypeToAverageCpuTime();
		//System.out.println("UsertupleTypeToAverageCpuTime "+this.getName()+ UsertupleTypeToAverageCpuTime);
		//System.out.println(" idOfEndDevices "+ idOfEndDevices);
		List< Map<String,Double>> LISTUsertupleTypeToAverageCpuTime;
		
		if(UsertupleTypeToAverageCpuTime.size()==this.idOfEndDevices.size()&& this.unsatisfiedUsers.size()>1) {
			for(Integer usr: this.idOfEndDevices) {
				//System.out.println(UsertupleTypeToAverageCpuTime.get(usr));
				LISTUsertupleTypeToAverageCpuTime= UsertupleTypeToAverageCpuTime.get(usr);
				for(Map<String,Double> mod: LISTUsertupleTypeToAverageCpuTime) {
					for (Map.Entry<String,Double> entry : mod.entrySet()) {  
						if(entry.getValue()> 15 && this.unsatisfiedUsers.containsKey(usr)) {
							//if(this.unsatisfiedUsers.containsKey(usr)) {
								//List<String> badmod=new ArrayList<String>();
								List<String> badmod=this.unsatisfiedUsers.get(usr);
								if(!badmod.contains(entry.getKey()))
										badmod.add(entry.getKey());
								this.unsatisfiedUsers.put(usr, badmod);
						}
							/*}
							else {
								List<String> badmod=new ArrayList<String>();
								badmod.add(entry.getKey());
								this.unsatisfiedUsers.put(usr, badmod);
							}*/
					}}}}

		Map<Integer, List<Pair<Integer,AppModule>>> devmodmapusr=  mymoduleplacement.getDeviceToModuleMapuser();
		List<Pair<Integer,AppModule>> modmapusr = devmodmapusr.get(this.getId());

		
		if(!this.unsatisfiedUsers.isEmpty()& !this.getName().startsWith("m")) {
			for(Integer usrtuples: unsatisfiedUsers.keySet()) {
				List<String> tuples =unsatisfiedUsers.get(usrtuples);
				List<AppModule> vms= new ArrayList<AppModule>();
				for(Pair<Integer,AppModule> usrmodules: modmapusr) {					
					if (usrmodules.getKey()==usrtuples) {						
						vms.add(usrmodules.getValue());					
					}
				}			
				unsatisfiedvms.put(usrtuples, vms);
			}}

		/*for(String tuple: tuples) {
			if(tuple.equals("SENSOR_DATA")) vms.add("Filtering");
			if(tuple.equals("FILTERED_DATA")) vms.add("Monitoring");
			if(tuple.equals("ECG_REPORT")) vms.add("Caregiver");
		}*/

		if(!this.unsatisfiedvms.isEmpty()& !this.getName().startsWith("m")) {
			for(Integer usr: unsatisfiedvms.keySet()) {
				//System.out.println(this.getName()+ this.unsatisfiedvms);
				List<AppModule> vms =unsatisfiedvms.get(usr);
				for(AppModule vm: vms) {
					MYupdateAllocatedMips(vm);
					//System.out.print("  "+usr+"" + myvm.getName()+ " " + myvm.getUid());
				}
				//System.out.println();
			}
			//this.unsatisfiedUsers.clear();
		}
		
} // placement = "Enhanced Latency Differentiated Module Placement" 
		
		/*if(!this.unsatisfiedUsers.isEmpty()& !this.getName().startsWith("m")) {
			System.out.println("=========================================");
			System.out.println(" List of Unsatisfied Users  ");
			System.out.print(this.getName() +" :  ");
			System.out.println(this.unsatisfiedUsers);
			System.out.println(this.unsatisfiedvms);
			}*/			
//////////////////////////////////end ragaanew check users delay /////////////////////////////
				
//////////////////////////////////end ragaanew check users delay /////////////////////////////
		/** Remove completed VMs **/		/** Change made by HARSHIT GUPTA		*/
		for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}	
		Log.printLine();
		setLastProcessTime(currentTime);
		return minTime;
	}
	
////////////////////////////checkdeviceperformance enhanced //////////////////////////////////////////////////////////
	public void checkDevicePerformanceEnhanced() {	
		//System.out.println("fogdevice=========checkDevicePerformanceEnhanced============="+this.getName() + "  "+ this.getId()+" utilizationofcpu "+ this.getHost().getUtilizationOfCpu());//+ " host "+  this.getHost().getId() this.getHost().getPower()+ " utilization "+ this.getHost().getUtilizationOfCpuMips()+this.getHost().getUtilizationOfCpu()+ " getEnergyConsumption()" + getEnergyConsumption());
		FogDevice PDev= null; PowerHost PHost= null; Pair<Integer, Vm> migrated = new Pair<Integer, Vm>(null,null);
		switch (this.getName()) { 
			case "cloud" :{ 
				//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
				//System.out.println("cloud"+ this.getName()+ this.getHost().getUtilizationOfCpu());
				break;
			}
			
			case "proxy-server" :{ 
				break;
				}
				/*//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
					//System.out.println(this.getName() + " appToModulesMapUsers "+this.appToModulesMapUsers.get("ECG"));
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();
					if (this.Executing==true) {
						System.out.println(this.getName() + " executing ");
						break;
						}
					List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");
					Integer x= pp.get(pp.size()-1).getKey(); // lowest priority user is migrated
					List<Pair<Integer, String>>dumpp= new ArrayList<Pair<Integer, String>>(); 
					Pair<Integer, String> miguser= null;
					for(Pair<Integer, String> y: pp)
						if (y.getKey()==x)
							dumpp.add(y);
					for(Pair<Integer, String> dummiguser: dumpp) {
						if (dummiguser.getValue().equals("Caregiver")) { miguser= dummiguser; break;}
						if (dummiguser.getValue().equals("Monitoring")) { miguser= dummiguser; break;}
						if (dummiguser.getValue().equals("Filtering")) { miguser= dummiguser; break;}
					}					
					//System.out.println("dumpp " + dumpp +"  miguser" + miguser);
					AppModule myvm = getModuleByName(miguser.getValue());////Integer id= miguser.getKey();
					
					Map<String, Application> myapplicationMap= applicationMap;						
					Application app = (Application)myapplicationMap.get("ECG");					
					sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
					sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);					
					Object[] data = new Object[3];
					data[0] =(Vm)myvm;
					data[1]= miguser.getKey(); // mobileid of the module
					data[2] = 1;
					sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();
					//if(myvm.getName()=="Caregiver"){data[2] = ++cinst;	}
					migrated = new Pair<Integer, Vm>(miguser.getKey(), (Vm)myvm);
					System.out.println(this.getName() + " CPU utilization "+ this.getHost().getUtilizationOfCpu()+ " Migrating lowest priority user "+  migrated.getKey()+" Module "+((AppModule)migrated.getValue()).getName()+"  to device " + PDev.getName() + " Per_User Basic Module Placement"); //+ placement				
				break;
				}
				*/
			case "d-"+ 0 :
			case "d-"+ 1 :
			case "d-"+ 2 :
			case "d-"+ 3 :{
				break;
			}
				/*	//System.out.println(this.getName() + " appToModulesMapUsers "+this.appToModulesMapUsers.get("ECG"));
					if(this.placement.equals("Latency Module Placement")) {
						PDev= (FogDevice)CloudSim.getEntity(this.getParentId());						
						PHost=  PDev.getHost();						
						if(PHost.getUtilizationOfCpu() == 0.0)  {
							System.out.println(this.getName()+ " NOT Migrating "+ this.getHost().getUtilizationOfCpu()+ " proxy utilization "+ PHost.getUtilizationOfCpu());
							break;
							}												
						//if(PHost.getUtilizationOfCpu()>0.90) {							
							//PDev= (FogDevice)CloudSim.getEntity("cloud");
							//PHost=  PDev.getHost();
							//System.out.println(this.getName()+ " "+ placement +" Proxy-server is full, migrating modules to "+ PDev.getName());
							//}
						}
					if(placement.equals("Latency Modified Module Placement")) {
						PDev= (FogDevice)CloudSim.getEntity("cloud");
						PHost=  PDev.getHost();
						//System.out.println("Migrating modules out of device "+this.getName()+" to "+ PDev.getName()+ "  "+placement);
						}
					if (this.Executing==true) {	
						System.out.println("NOT Migrating "+ this.getName()+ " executing");
						break;
						}
					List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");
					//System.out.println("befor appToModulesMapUsers of last user "+ pp.get(pp.size()-1));
					Integer x= pp.get(pp.size()-1).getKey(); // lowest priority user is migrated
					List<Pair<Integer, String>>dumpp= new ArrayList<Pair<Integer, String>>(); 
					Pair<Integer, String> miguser= null;
					for(Pair<Integer, String> y: pp)
						if (y.getKey()==x)
							dumpp.add(y);
					for(Pair<Integer, String> dummiguser: dumpp) {
						if (dummiguser.getValue().equals("Monitoring")) { miguser= dummiguser; break;}
						if (dummiguser.getValue().equals("Filtering")) { miguser= dummiguser; break;}
					}					
					//System.out.println("dumpp " + dumpp +"  miguser" + miguser);					
					AppModule myvm= getModuleByName(miguser.getValue()); //AppModule MOD = myvm;
					Map<String, Application> myapplicationMap= applicationMap;						
					Application app = (Application)myapplicationMap.get("ECG");
					//System.out.println("applicationMap  " +applicationMap+ app);
					sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
					sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);					
					// code to update number of instances for each module at the dept  //////////////						
					Object[] data = new Object[3];
					data[0] =(Vm)myvm;
					data[1]= miguser.getKey(); // mobileid of the module
					data[2] = 1;
					sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();									
					migrated = new Pair<Integer, Vm>(miguser.getKey(), (Vm)myvm);
					System.out.println(this.getName() + " CPU utilization "+ this.getHost().getUtilizationOfCpu()+ " Migrating lowest priority user "+ migrated.getKey()+" Module "+((AppModule)migrated.getValue()).getName()+ " to device " + PDev.getName()+ "  Per_User Basic Module Placement"); //+placement 					
				break;
			}
		*/
			default:{ 
					//System.out.println("mobile "+ this.getName()+" "+this.getHost().getUtilizationOfCpu());// this.appToModulesMap);	//System.out.println("mobile "+ this.getName()+" moduledeadline "+ this.moduledeadline+ "  deadlineInfomodule  " + this.deadlineInfomodule);
					//if(this.getId()==18) { //.getName().equals("m-0-4")) {	//this.getId()==6) //System.out.println("m-0-0 is out "+this.getId()+ this.getName());
						//System.out.println(this.getName()+ " vmlist size"+ this.getHost().getVmList().size() + this.getHost().getUtilizationOfCpu() );
						//break; //}
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();
					if (this.Executing==true) {
						//System.out.println("executing");
						break;
						}
					for(AppModule myvm : this.Appmoduledeadline.keySet()) {	
						//System.out.println(this.getName()+" CPU utilization "+this.getHost().getUtilizationOfCpu()+"  Migrating out " +myvm.getName()+ " to "+ PDev.getName()+" "+ placement);					
						Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
						migrate.put("vm", (Vm)myvm);
						migrate.put("host",PHost);
						migrate.put("datacenter",PHost.getDatacenter());
						//System.out.println(this.getName()+" datacenterid " +this.getId()+" hostid "+this.getHost().getId()+ " will migrate vm "+ myvm.getId()+ MOD.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
						sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);						
						Map<String, Application> myapplicationMap= applicationMap;						
						Application app = (Application)myapplicationMap.get("ECG");
						sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
						sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);
						// code to update number of instances for each module at the dept  //////////////						
							Object[] data = new Object[3];
							data[0] =(Vm)myvm;
							data[1]= this.getId(); 
							data[2] = 1;	//System.out.println(this.getName()+incomingOperator+ data[0]+data[1]);
							sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();							
							//if(myvm.getName()=="Filtering"){}if(myvm.getName()=="Monitoring"){data[2] = ++minst;sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data); } // calls updateModuleInstanceCount();			
							migrated = new Pair<Integer, Vm>(this.getId(), (Vm)myvm);
							this.Appmoduledeadline.remove(myvm);			
							System.out.println(this.getName()+ " migrated " + ((AppModule)migrated.getValue()).getName());
							break;					
						}				
			}// end default 
		}// end switch case
		if(migrated.getKey()!= null) {			//Pair<Integer, Vm> migrated	 
			this.appToModulesMap.get("ECG").remove(((AppModule)migrated.getValue()).getName());
			this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
			this.getVmList().remove(migrated.getValue());
			for (PowerHost host : this.<PowerHost> getHostList()) {
				for (Vm vm : host.getCompletedVms()) {
					getVmAllocationPolicy().deallocateHostForVm(vm);
					getVmList().remove(vm);
					Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
				}
			}			
			String modnam = ((AppModule)migrated.getValue()).getName();
			Pair<Integer,String>migratednam= new Pair<Integer,String>(migrated.getKey(), modnam);
			this.appToModulesMapUsers.get("ECG").remove(migratednam);
			//System.out.println(this.getName()+ " migrated " + ((AppModule)migrated.getValue()).getName());// +" appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);					
			}
	}
//////////////////////////////////////////////////////////////////////////////////////////////
	private void checkDevicePerformanceEdgewardEnhanced() {
		//System.out.println("============================================================== ");
		//System.out.println("checkDevicePerformanceEdgeward enhanced =============================== "+this.getName());
		FogDevice PDev= null; PowerHost PHost= null; Pair<Integer, Vm> migrated = new Pair<Integer, Vm>(null,null);
		switch (this.getName()) { 
			case "cloud" :{  
				//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
				//System.out.println("cloud"+ this.getName()+ this.getHost().getUtilizationOfCpu()+ " vmlist size "+this.getHost().getVmList().size()+ " appToModulesMapUsers " +this.appToModulesMapUsers.get("ECG").size());
				break;
			}
			case "proxy-server" :{				
				//System.out.println(this.getName()+ " "+ this.getHost().getUtilizationOfCpu() );//+ " vmlist size "+this.getHost().getVmList().size()+"appToModulesMapUsers "+ this.appToModulesMapUsers.get("ECG").size()+  this.appToModulesMapUsers.get("ECG"));
				//System.out.println("appToModulesMapUsers "+ this.appToModulesMapUsers.get("ECG").size()+  this.appToModulesMapUsers.get("ECG"));//+" moduledeadline " + this.moduledeadline);					
				if(this.moduledeadline.size() > 0) {														
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();					
					if (this.Executing==true) {
						System.out.println("proxy-server executing");
						break;
						}
					for(Vm vm:  this.getHost().getVmList()) {
						//System.out.println(this.getName()+ vm.getId()+ ((AppModule)vm).getName() +" AllocatedMipsForVm "+this.getHost().getAllocatedMipsForVm(vm)+" CloudletScheduler CurrentRequestedMips  "+vm.getCloudletScheduler().getCurrentRequestedMips());
					}
					
					Map<String, Application> myapplicationMap= applicationMap;						
					Application app = (Application)myapplicationMap.get("ECG");					
					sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); 					
					
					Map.Entry<String,Double> entry = this.moduledeadline.entrySet().iterator().next();
					 String key = entry.getKey();
					 Double value = entry.getValue();
					 //System.out.println(this.getName()+ " moduledeadline  key " + key +" value "+ value);
					 
					 List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");				 
					 AppModule mymod= null; 
					 List<Pair<Integer, String>>remusrlist= new ArrayList<Pair<Integer,String>>();
					 List<String>remumodlist= new ArrayList<String>();
					 List<Vm>remvmlist= new ArrayList<Vm>();  //System.out.println(" pp  " + pp);
					 
					for(Vm myvm:  this.getHost().getVmList()) { 
					 //for(Vm myvm:  cleanvm) { 	 
						 String nam= ((AppModule)myvm).getName();
						 for(Pair<Integer, String> miguser: pp) {
							 //System.out.println(" miguser " + miguser + key +" "+miguser.getValue().equals(key));
							 if((nam.equals(key))&& (miguser.getValue().equals(key))) {								
								//myvm= getModuleByName(miguser.getValue()); //AppModule MOD = myvm;
								remusrlist.add(miguser);
								remumodlist.add(miguser.getValue());
								if(! remvmlist.contains(myvm))
									remvmlist.add(myvm);						
								mymod= (AppModule)myvm;
								
								Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
								migrate.put("vm", mymod);
								migrate.put("host",PHost);
								migrate.put("datacenter",PHost.getDatacenter());
								sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);
								
								sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, mymod); //calls processModuleArrival(ev);					
								
								// code to update number of instances for each module at the dept  //////////////																
								Object[] data = new Object[3];
								data[0] =mymod;
								data[1]= 0; // mobileid of the module
								data[2]= 1;
								sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();						
							}
						}					 
					 }
					 //System.out.println("remusrlist "+ remusrlist + "remumodlist "+	remumodlist+"	remvmlist"+		remvmlist);												
						
						moduledeadline.remove(key);				
						
						this.appToModulesMapUsers.get("ECG").removeAll(remusrlist);				
						this.appToModulesMap.get("ECG").removeAll(remumodlist);
						//System.out.println("VmList size befor " + this.getVmList().size()+ " "+ this.getVmList());
						//System.out.println("remvmlist " + remvmlist.size());
						for (Vm vm:remvmlist) {
							//System.out.println(" vm  " + vm);
							this.getVmAllocationPolicy().deallocateHostForVm(vm);
							this.getVmList().remove(vm);
						}
						//System.out.println("VmList size after " + this.getVmList().size()+ " "+ this.getVmList());
						//System.out.println("================================================");
						System.out.println(this.getName()+ " CPU utilization "+this.getHost().getUtilizationOfCpu()+" Migrating the whole module "+ key + " to device " + PDev.getName()+" "+placement );
						//System.out.println("================================================");
						//System.out.println("appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);
						//System.out.println(" new moduledeadline " +moduledeadline);
						}										
				break;
				} 
			case "d-"+ 0 :
			case "d-"+ 1 :
			case "d-"+ 2 :
			case "d-"+ 3 :{											
				//System.out.println(this.getName()+ " "+this.getHost().getUtilizationOfCpu());//+" moduledeadline " + this.moduledeadline);				
				//System.out.println(this.getName()+ " vmlist " + this.getHost().getVmList().size()+ " moduledeadline.size " + this.moduledeadline.size()+ " utilization "+this.getHost().getUtilizationOfCpu() +"  appToModulesMapUsers "+ this.appToModulesMapUsers.get("ECG").size()+  this.appToModulesMapUsers);
				if(this.moduledeadline.size() > 0) {			
					//System.out.println(this.getName()+ " vmlist " + this.getHost().getVmList().size()+ " utilization "+this.getHost().getUtilizationOfCpu());					
					if (this.Executing==true) {
						System.out.println("Dept executing");
						break;
						}
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());						
					PHost=  PDev.getHost();
					//System.out.println(this.getName()+ " "+this.getHost().getUtilizationOfCpu()+ " proxy utilization "+ PHost.getUtilizationOfCpu());
					double PHostUtilizationOfCpu = PHost.getUtilizationOfCpu();
					if(PHostUtilizationOfCpu == 0.0)  {System.out.println("Parent Host utilization = "+ PHostUtilizationOfCpu); break;}
					else if(PHostUtilizationOfCpu > 0.70) { //70 + this.getHost().getAllocatedMipsForVm(vm);							
						//PDev= (FogDevice)CloudSim.getEntity("cloud");
						//PHost=  PDev.getHost();
						//System.out.println(this.getName()+ " "+ placement +" Proxy-server is full, asking proxy to get ready so dept can migrate the whole module to proxy "+ PDev.getName());
						}
					for(Vm vm:  this.getHost().getVmList()) {
						//System.out.println(this.getName()+ vm.getId()+ ((AppModule)vm).getName() +" AllocatedMipsForVm "+this.getHost().getAllocatedMipsForVm(vm)+" CloudletScheduler CurrentRequestedMips  "+vm.getCloudletScheduler().getCurrentRequestedMips());
					}
				Map<String, Application> myapplicationMap= applicationMap;						
				Application app = (Application)myapplicationMap.get("ECG");
				sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();					
				
				 Map.Entry<String,Double> entry = this.moduledeadline.entrySet().iterator().next();
				 String key = entry.getKey();
				 Double value = entry.getValue();
				// System.out.println(this.getName()+ " moduledeadline  key " + key +" value "+ value);				 
				 
				 List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");				 
				 AppModule mymod= null; 
				 List<Pair<Integer, String>>remusrlist= new ArrayList<Pair<Integer,String>>();
				 List<String>remumodlist= new ArrayList<String>();
				 List<Vm>remvmlist= new ArrayList<Vm>();  //System.out.println(" pp  " + pp);				 
				 for(Vm myvm:  this.getHost().getVmList()) { 
					 String nam= ((AppModule)myvm).getName();
					 for(Pair<Integer, String> miguser: pp) {
						 //System.out.println(" miguser " + miguser + key +" "+miguser.getValue().equals(key));
						 if((nam.equals(key))&& (miguser.getValue().equals(key))) {							
							//myvm= getModuleByName(miguser.getValue()); //AppModule MOD = myvm;
							remusrlist.add(miguser);
							remumodlist.add(miguser.getValue());
							if(!remvmlist.contains(myvm)) {
								remvmlist.add(myvm);								
								}
							}						
						}					 
					 }												
						//	try {
				 		for(Vm myvm : remvmlist) {
							myvm.setInMigration(true);							
							mymod= (AppModule)myvm;
							Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
							migrate.put("vm", mymod);
							migrate.put("host",PHost);
							migrate.put("datacenter",PHost.getDatacenter());
							sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);							
							sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, mymod); //calls processModuleArrival(ev);
							// code to update number of instances for each module at the dept  //////////////						
							Object[] data = new Object[3];
							data[0] =mymod;
							data[1]= 0; // mobileid of the module
							data[2]= 1;
							sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();							
				 		}
					 
				// System.out.println(i + " remusrlist "+ remusrlist + "remumodlist "+	remumodlist+"	remvmlist"+	remvmlist);											
					
					moduledeadline.remove(key);					
					this.appToModulesMapUsers.get("ECG").removeAll(remusrlist);				
					this.appToModulesMap.get("ECG").removeAll(remumodlist);
				//	System.out.println("VmList size befor " + this.getVmList().size());
				//	System.out.println("remvmlist " + remvmlist.size());
					for (Vm vm:remvmlist) {
						//System.out.println(" vm  " + vm);					
						this.getVmAllocationPolicy().deallocateHostForVm(vm);
						this.getVmList().remove(vm);
					}
					//System.out.println(" hiiiiiiii");
					//System.out.println("================================================");
					System.out.println(this.getName()+ " CPU utilization "+this.getHost().getUtilizationOfCpu() +" Migrating the whole module "+ key + " to device " + PDev.getName()+"  "+placement );
					//System.out.println("================================================");
					//System.out.println("appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);
					//System.out.println("VmList size after " + this.getVmList().size()+ " "+ this.getVmList());
					//System.out.println(" new moduledeadline " +moduledeadline);
					}
			break;
			} //
			default:{  	
				//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
				//System.out.println("mobile "+ this.getName()+" " +this.getHost().getUtilizationOfCpu()+ " "+this.getHost().getVmList().size()+ this.appToModulesMap);	//System.out.println("mobile "+ this.getName()+" moduledeadline "+ this.moduledeadline+ "  deadlineInfomodule  " + this.deadlineInfomodule);					
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();								
					for(AppModule myvm : this.Appmoduledeadline.keySet()) {
						//System.out.println("=================================");
						//System.out.println("try to Migrate " +myvm.getName()+ " out of mobile "+ this.getId()+ " "+this.getName()+ " to "+ PDev.getName());						
						if (this.Executing==true) {
							//System.out.println("executing");
							break;
							}
						for(Vm vm:  this.getHost().getVmList()) {
							//System.out.println(this.getName()+ vm.getId()+ ((AppModule)vm).getName() +" AllocatedMipsForVm "+this.getHost().getAllocatedMipsForVm(vm)+" CloudletScheduler CurrentRequestedMips  "+vm.getCloudletScheduler().getCurrentRequestedMips());
						}
						
						try {
						myvm.setInMigration(true);	
						Map<String, Application> myapplicationMap= applicationMap;						
						Application app = (Application)myapplicationMap.get("ECG");
						sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
											
						Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
						migrate.put("vm", (Vm)myvm);
						migrate.put("host",PHost);
						migrate.put("datacenter",PHost.getDatacenter());						
						//System.out.println(this.getName()+" datacenterid " +this.getId()+" hostid "+this.getHost().getId()+ " will migrate vm "+ myvm.getId()+ myvm.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
						//System.out.println(this.getName()+ " will migrate vm "+ myvm.getId()+ myvm.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
						sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);
						
						sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);
						// code to update number of instances for each module at the dept  //////////////						
						Object[] data = new Object[3];
						data[0] =(Vm)myvm;
						data[1]= this.getId(); 
						data[2] = 1;	//System.out.println(this.getName()+incomingOperator+ data[0]+data[1]);
						sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();
					}
					catch (Exception e)
					{
						//System.out.println("========================"); System.out.println("migration interupted");System.out.println("========================");
					}							
						migrated = new Pair<Integer, Vm>(this.getId(), (Vm)myvm);

						this.appToModulesMap.get("ECG").remove(((AppModule)migrated.getValue()).getName());
						this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
						this.getVmList().remove(migrated.getValue());
						
						String modnam = ((AppModule)migrated.getValue()).getName();
						Pair<Integer,String>migratednam= new Pair<Integer,String>(migrated.getKey(), modnam);
						this.appToModulesMapUsers.get("ECG").remove(migratednam);
						System.out.println(this.getName()+" CPU utilization "+this.getHost().getUtilizationOfCpu()+ " migrated " + myvm.getName());//" appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);
						
						this.Appmoduledeadline.remove(myvm);
						break;					
						}						
//}// end mobile device   
			}// end default 
		}// end switch case		
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
////////////////////////ragaa   checkDevicePerformance()///////////////////////////////
	public void checkDevicePerformance() {	
		//System.out.println("fogdevice=========checkDevicePerformance============="+this.getName() + "  "+ this.getId()+" utilizationofcpu "+ this.getHost().getUtilizationOfCpu());//+ " host "+  this.getHost().getId() this.getHost().getPower()+ " utilization "+ this.getHost().getUtilizationOfCpuMips()+this.getHost().getUtilizationOfCpu()+ " getEnergyConsumption()" + getEnergyConsumption());
		FogDevice PDev= null; PowerHost PHost= null; Pair<Integer, Vm> migrated = new Pair<Integer, Vm>(null,null);
		switch (this.getName()) { 
			case "cloud" :{ 
				//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
				//System.out.println("cloud"+ this.getName()+ this.getHost().getUtilizationOfCpu());
				break;
			}
			case "proxy-server" :{
				break;
			}
			/*	//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
				if(this.getHost().getUtilizationOfCpu()>= 0.90) { //if (this.getHost().getPower()>= 100) {	//if (getEnergyConsumption()> 5000) {
					//System.out.println(this.getName() + " appToModulesMapUsers "+this.appToModulesMapUsers.get("ECG"));
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();
					if (this.Executing==true) {
						mig=true;
						break;
						}
					List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");
					Integer x= pp.get(pp.size()-1).getKey(); // lowest priority user is migrated
					List<Pair<Integer, String>>dumpp= new ArrayList<Pair<Integer, String>>(); 
					Pair<Integer, String> miguser= null;
					for(Pair<Integer, String> y: pp)
						if (y.getKey()==x)
							dumpp.add(y);
					for(Pair<Integer, String> dummiguser: dumpp) {
						if (dummiguser.getValue().equals("Caregiver")) { miguser= dummiguser; break;}
						if (dummiguser.getValue().equals("Monitoring")) { miguser= dummiguser; break;}
						if (dummiguser.getValue().equals("Filtering")) { miguser= dummiguser; break;}
					}					
					//System.out.println("dumpp " + dumpp +"  miguser" + miguser);
					AppModule myvm = getModuleByName(miguser.getValue());////Integer id= miguser.getKey();
					
					Map<String, Application> myapplicationMap= applicationMap;						
					Application app = (Application)myapplicationMap.get("ECG");					
					sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
					sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);					
					Object[] data = new Object[3];
					data[0] =(Vm)myvm;
					data[1]= miguser.getKey(); // mobileid of the module
					data[2] = 1;
					sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();
					//if(myvm.getName()=="Caregiver"){data[2] = ++cinst;	}
					migrated = new Pair<Integer, Vm>(miguser.getKey(), (Vm)myvm);
					System.out.println(this.getName() + " CPU utilization "+ this.getHost().getUtilizationOfCpu()+ " Migrating lowest priority user "+  migrated.getKey()+" Module "+((AppModule)migrated.getValue()).getName()+"  to device " + PDev.getName() + " Per_User Basic Module Placement"); //+ placement
					mig=false;
					}        //				
				break;
				}
				*/
			case "d-"+ 0 :
			case "d-"+ 1 :
			case "d-"+ 2 :
			case "d-"+ 3 :{
				break;
			}
			/*	if(this.getHost().getUtilizationOfCpu()>= 0.90) { //if (this.getHost().getPower()>= 100) {	//if (getEnergyConsumption()> 5000) {
					//System.out.println(this.getName() + " appToModulesMapUsers "+this.appToModulesMapUsers.get("ECG"));
					if(this.placement.equals("Latency Module Placement")) {
						PDev= (FogDevice)CloudSim.getEntity(this.getParentId());						
						PHost=  PDev.getHost();						
						if(PHost.getUtilizationOfCpu() == 0.0)  {
							System.out.println("NOT Migrating "+this.getName()+ " "+this.getHost().getUtilizationOfCpu()+ " proxy utilization "+ PHost.getUtilizationOfCpu());
							mig=true; 
							break;
							}												
						if(PHost.getUtilizationOfCpu()>0.90) {							
							PDev= (FogDevice)CloudSim.getEntity("cloud");
							PHost=  PDev.getHost();
							//System.out.println(this.getName()+ " "+ placement +" Proxy-server is full, migrating modules to "+ PDev.getName());
							}
						}
					if(placement.equals("Latency Modified Module Placement")) {
						PDev= (FogDevice)CloudSim.getEntity("cloud");
						PHost=  PDev.getHost();
						//System.out.println("Migrating modules out of device "+this.getName()+" to "+ PDev.getName()+ "  "+placement);
						}
					if (this.Executing==true) {	
						System.out.println("NOT Migrating executing");
						mig=true;
						break;
						}
					List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");
					//System.out.println("befor appToModulesMapUsers of last user "+ pp.get(pp.size()-1));
					Integer x= pp.get(pp.size()-1).getKey(); // lowest priority user is migrated
					List<Pair<Integer, String>>dumpp= new ArrayList<Pair<Integer, String>>(); 
					Pair<Integer, String> miguser= null;
					for(Pair<Integer, String> y: pp)
						if (y.getKey()==x)
							dumpp.add(y);
					for(Pair<Integer, String> dummiguser: dumpp) {
						if (dummiguser.getValue().equals("Monitoring")) { miguser= dummiguser; break;}
						if (dummiguser.getValue().equals("Filtering")) { miguser= dummiguser; break;}
					}					
					//System.out.println("dumpp " + dumpp +"  miguser" + miguser);					
					AppModule myvm= getModuleByName(miguser.getValue()); //AppModule MOD = myvm;
					Map<String, Application> myapplicationMap= applicationMap;						
					//Application app = (Application)myapplicationMap.get("ECG");
					//System.out.println("applicationMap  " +applicationMap+ app);
					//sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
					sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);					
					// code to update number of instances for each module at the dept  //////////////						
					Object[] data = new Object[3];
					data[0] =(Vm)myvm;
					data[1]= miguser.getKey(); // mobileid of the module
					data[2] = 1;
					sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();									
					migrated = new Pair<Integer, Vm>(miguser.getKey(), (Vm)myvm);
					System.out.println(this.getName() + " CPU utilization "+ this.getHost().getUtilizationOfCpu()+ " Migrating lowest priority user "+ migrated.getKey()+" Module "+((AppModule)migrated.getValue()).getName()+ " to device " + PDev.getName()+ "  Per_User Basic Module Placement"); //+placement 
					mig=false;
					} //
				break;
			}
			*/
			default:{ 
			//System.out.println("mobile "+ this.getName()+" "+this.getHost().getUtilizationOfCpu());// this.appToModulesMap);	//System.out.println("mobile "+ this.getName()+" moduledeadline "+ this.moduledeadline+ "  deadlineInfomodule  " + this.deadlineInfomodule);
			if(this.getHost().getUtilizationOfCpu()>= 0.3) { //if (this.getHost().getPower()>= 100) {	//if (getEnergyConsumption()> 5000) {
					//if(this.getId()==18) { //.getName().equals("m-0-4")) {	//this.getId()==6) //System.out.println("m-0-0 is out "+this.getId()+ this.getName());
						//System.out.println(this.getName()+ " vmlist size"+ this.getHost().getVmList().size() + this.getHost().getUtilizationOfCpu() );
						//break;
					//}
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();
					if (this.Executing==true) {
						//System.out.println("executing");
						break;
						}
					for(AppModule myvm : this.Appmoduledeadline.keySet()) {	
						//System.out.println(this.getName()+" CPU utilization "+this.getHost().getUtilizationOfCpu()+"  Migrating out " +myvm.getName()+ " to "+ PDev.getName()+" "+ placement);					
						Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
						migrate.put("vm", (Vm)myvm);
						migrate.put("host",PHost);
						migrate.put("datacenter",PHost.getDatacenter());
						//System.out.println(this.getName()+" datacenterid " +this.getId()+" hostid "+this.getHost().getId()+ " will migrate vm "+ myvm.getId()+ MOD.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
						sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);						
						Map<String, Application> myapplicationMap= applicationMap;						
						Application app = (Application)myapplicationMap.get("ECG");
						sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
						sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);
						// code to update number of instances for each module at the dept  //////////////						
							Object[] data = new Object[3];
							data[0] =(Vm)myvm;
							data[1]= this.getId(); 
							data[2] = 1;	//System.out.println(this.getName()+incomingOperator+ data[0]+data[1]);
							sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();							
							//if(myvm.getName()=="Filtering"){}if(myvm.getName()=="Monitoring"){data[2] = ++minst;sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data); } // calls updateModuleInstanceCount();			
							migrated = new Pair<Integer, Vm>(this.getId(), (Vm)myvm);
							this.Appmoduledeadline.remove(myvm);
							mig=false;
							break;					
						}						
					} // end mobile device  //
			}// end default 
		}// end switch case
		if(migrated.getKey()!= null) {			//Pair<Integer, Vm> migrated	 
			this.appToModulesMap.get("ECG").remove(((AppModule)migrated.getValue()).getName());
			this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
			this.getVmList().remove(migrated.getValue());
			
			String modnam = ((AppModule)migrated.getValue()).getName();
			Pair<Integer,String>migratednam= new Pair<Integer,String>(migrated.getKey(), modnam);
			this.appToModulesMapUsers.get("ECG").remove(migratednam);
			System.out.println(this.getName()+ " migrated " + ((AppModule)migrated.getValue()).getName());// +" appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);					
			}
	}
//////////////////////////////////////////////////////////////////////////////////////////////
	private void checkDevicePerformanceEdgeward() {
		//System.out.println("============================================================== ");
		//System.out.println("checkDevicePerformanceEdgeward =============================== "+this.getName());
		FogDevice PDev= null; PowerHost PHost= null; Pair<Integer, Vm> migrated = new Pair<Integer, Vm>(null,null);
		switch (this.getName()) { 
			case "cloud" :{  
				//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
				//System.out.println("cloud"+ this.getName()+ this.getHost().getUtilizationOfCpu()+ " vmlist size "+this.getHost().getVmList().size()+ " appToModulesMapUsers " +this.appToModulesMapUsers.get("ECG").size());
				break;
			}
			case "proxy-server" :{				
				//System.out.println(this.getName()+ " "+ this.getHost().getUtilizationOfCpu() );//+ " vmlist size "+this.getHost().getVmList().size()+"appToModulesMapUsers "+ this.appToModulesMapUsers.get("ECG").size()+  this.appToModulesMapUsers.get("ECG"));
				//System.out.println("appToModulesMapUsers "+ this.appToModulesMapUsers.get("ECG").size()+  this.appToModulesMapUsers.get("ECG"));//+" moduledeadline " + this.moduledeadline);					
				if(this.getHost().getUtilizationOfCpu()>= 0.9 &&(this.moduledeadline.size() > 0)) {
				if(this.moduledeadline.size() > 0) {														
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();					
					if (this.Executing==true) {
						System.out.println("proxy-server executing");
						mig=true;
						break;
						}
					for(Vm vm:  this.getHost().getVmList()) {
						//System.out.println(this.getName()+ vm.getId()+ ((AppModule)vm).getName() +" AllocatedMipsForVm "+this.getHost().getAllocatedMipsForVm(vm)+" CloudletScheduler CurrentRequestedMips  "+vm.getCloudletScheduler().getCurrentRequestedMips());
					}
					
					Map<String, Application> myapplicationMap= applicationMap;						
					Application app = (Application)myapplicationMap.get("ECG");					
					sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); 					
					
					Map.Entry<String,Double> entry = this.moduledeadline.entrySet().iterator().next();
					 String key = entry.getKey();
					 Double value = entry.getValue();
					 //System.out.println(this.getName()+ " moduledeadline  key " + key +" value "+ value);
					 
					 List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");				 
					 int i=0;	AppModule mymod= null; 
					 List<Pair<Integer, String>>remusrlist= new ArrayList<Pair<Integer,String>>();
					 List<String>remumodlist= new ArrayList<String>();
					 List<Vm>remvmlist= new ArrayList<Vm>();  //System.out.println(" pp  " + pp);
					 
					for(Vm myvm:  this.getHost().getVmList()) { 
					 //for(Vm myvm:  cleanvm) { 	 
						 String nam= ((AppModule)myvm).getName();
						 for(Pair<Integer, String> miguser: pp) {
							 //System.out.println(" miguser " + miguser + key +" "+miguser.getValue().equals(key));
							 if((nam.equals(key))&& (miguser.getValue().equals(key))) {								
								//myvm= getModuleByName(miguser.getValue()); //AppModule MOD = myvm;
								remusrlist.add(miguser);
								remumodlist.add(miguser.getValue());
								if(! remvmlist.contains(myvm))
									remvmlist.add(myvm);						
								mymod= (AppModule)myvm;
								
								Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
								migrate.put("vm", mymod);
								migrate.put("host",PHost);
								migrate.put("datacenter",PHost.getDatacenter());
								sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);
								
								sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, mymod); //calls processModuleArrival(ev);					
								
								// code to update number of instances for each module at the dept  //////////////																
								Object[] data = new Object[3];
								data[0] =mymod;
								data[1]= 0; // mobileid of the module
								data[2]= 1;
								sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();						
							}
						}					 
					 }
					 //System.out.println("remusrlist "+ remusrlist + "remumodlist "+	remumodlist+"	remvmlist"+		remvmlist);												
						
						moduledeadline.remove(key);				
						
						this.appToModulesMapUsers.get("ECG").removeAll(remusrlist);				
						this.appToModulesMap.get("ECG").removeAll(remumodlist);
						//System.out.println("VmList size befor " + this.getVmList().size()+ " "+ this.getVmList());
						//System.out.println("remvmlist " + remvmlist.size());
						for (Vm vm:remvmlist) {
							//System.out.println(" vm  " + vm);
							this.getVmAllocationPolicy().deallocateHostForVm(vm);
							this.getVmList().remove(vm);
						}
						//System.out.println("VmList size after " + this.getVmList().size()+ " "+ this.getVmList());
						//System.out.println("================================================");
						System.out.println(this.getName()+ " CPU utilization "+this.getHost().getUtilizationOfCpu()+" Migrating the whole module "+ key + " to device " + PDev.getName()+" "+placement );
						//System.out.println("================================================");
						//System.out.println("appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);
		//				System.out.println(" new moduledeadline " +moduledeadline);
						}
				mig=false;						
					} 
				break;
				} 
			case "d-"+ 0 :
			case "d-"+ 1 :
			case "d-"+ 2 :
			case "d-"+ 3 :{											
				//System.out.println(this.getName()+ " "+this.getHost().getUtilizationOfCpu());//+" moduledeadline " + this.moduledeadline);				
				//System.out.println(this.getName()+ " vmlist " + this.getHost().getVmList().size()+ " moduledeadline.size " + this.moduledeadline.size()+ " utilization "+this.getHost().getUtilizationOfCpu() +"  appToModulesMapUsers "+ this.appToModulesMapUsers.get("ECG").size()+  this.appToModulesMapUsers);
				
				if((this.getHost().getUtilizationOfCpu()>= 0.90) && (this.moduledeadline.size() > 0)) {
				if(this.moduledeadline.size() > 0) {			
					//System.out.println(this.getName()+ " vmlist " + this.getHost().getVmList().size()+ " utilization "+this.getHost().getUtilizationOfCpu());					
					if (this.Executing==true) {
						System.out.println("Dept executing");
						mig=true;
						break;
						}
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());						
					PHost=  PDev.getHost();
					//System.out.println(this.getName()+ " "+this.getHost().getUtilizationOfCpu()+ " proxy utilization "+ PHost.getUtilizationOfCpu());
					double PHostUtilizationOfCpu = PHost.getUtilizationOfCpu();
					if(PHostUtilizationOfCpu == 0.0)  {System.out.println("Parent Host utilization = "+ PHostUtilizationOfCpu); break;}
					else if(PHostUtilizationOfCpu > 0.70) { //70 + this.getHost().getAllocatedMipsForVm(vm);							
						//PDev= (FogDevice)CloudSim.getEntity("cloud");
						//PHost=  PDev.getHost();
						//System.out.println(this.getName()+ " "+ placement +" Proxy-server is full, asking proxy to get ready so dept can migrate the whole module to proxy "+ PDev.getName());
						}
					for(Vm vm:  this.getHost().getVmList()) {
						//System.out.println(this.getName()+ vm.getId()+ ((AppModule)vm).getName() +" AllocatedMipsForVm "+this.getHost().getAllocatedMipsForVm(vm)+" CloudletScheduler CurrentRequestedMips  "+vm.getCloudletScheduler().getCurrentRequestedMips());
					}
				Map<String, Application> myapplicationMap= applicationMap;						
				Application app = (Application)myapplicationMap.get("ECG");
				sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();					
				
				 Map.Entry<String,Double> entry = this.moduledeadline.entrySet().iterator().next();
				 String key = entry.getKey();
				 Double value = entry.getValue();
				// System.out.println(this.getName()+ " moduledeadline  key " + key +" value "+ value);				 
				 
				 List<Pair<Integer, String>>pp= this.appToModulesMapUsers.get("ECG");				 
				 AppModule mymod= null; 
				 List<Pair<Integer, String>>remusrlist= new ArrayList<Pair<Integer,String>>();
				 List<String>remumodlist= new ArrayList<String>();
				 List<Vm>remvmlist= new ArrayList<Vm>();  //System.out.println(" pp  " + pp);				 
				 for(Vm myvm:  this.getHost().getVmList()) { 
					 String nam= ((AppModule)myvm).getName();
					 for(Pair<Integer, String> miguser: pp) {
						 //System.out.println(" miguser " + miguser + key +" "+miguser.getValue().equals(key));
						 if((nam.equals(key))&& (miguser.getValue().equals(key))) {							
							//myvm= getModuleByName(miguser.getValue()); //AppModule MOD = myvm;
							remusrlist.add(miguser);
							remumodlist.add(miguser.getValue());
							if(!remvmlist.contains(myvm)) {
								remvmlist.add(myvm);								
								}
							}						
						}					 
					 }												
						//	try {
				 		for(Vm myvm : remvmlist) {
							myvm.setInMigration(true);							
							mymod= (AppModule)myvm;
							Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
							migrate.put("vm", mymod);
							migrate.put("host",PHost);
							migrate.put("datacenter",PHost.getDatacenter());
							sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);							
							sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, mymod); //calls processModuleArrival(ev);
							// code to update number of instances for each module at the dept  //////////////						
							Object[] data = new Object[3];
							data[0] =mymod;
							data[1]= 0; // mobileid of the module
							data[2]= 1;
							sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();							
				 		}
					 
				// System.out.println(i + " remusrlist "+ remusrlist + "remumodlist "+	remumodlist+"	remvmlist"+	remvmlist);											
					
					moduledeadline.remove(key);					
					this.appToModulesMapUsers.get("ECG").removeAll(remusrlist);				
					this.appToModulesMap.get("ECG").removeAll(remumodlist);
				//	System.out.println("VmList size befor " + this.getVmList().size());
				//	System.out.println("remvmlist " + remvmlist.size());
					for (Vm vm:remvmlist) {
						//System.out.println(" vm  " + vm);					
						this.getVmAllocationPolicy().deallocateHostForVm(vm);
						this.getVmList().remove(vm);
					}
					//System.out.println(" hiiiiiiii");
					//System.out.println("================================================");
					System.out.println(this.getName()+ " CPU utilization "+this.getHost().getUtilizationOfCpu() +" Migrating the whole module "+ key + " to device " + PDev.getName()+"  "+placement );
					//System.out.println("================================================");
					//System.out.println("appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);
					//System.out.println("VmList size after " + this.getVmList().size()+ " "+ this.getVmList());
					//System.out.println(" new moduledeadline " +moduledeadline);
					}
				mig=false;
					}
			break;
			} //
			default:{  	
				//this.Cputilization.put(CloudSim.clock(),this.getHost().getUtilizationOfCpu()); //Map<Double, Double>(); // device cpu utilization
				//System.out.println("mobile "+ this.getName()+" " +this.getHost().getUtilizationOfCpu()+ " "+this.getHost().getVmList().size()+ this.appToModulesMap);	//System.out.println("mobile "+ this.getName()+" moduledeadline "+ this.moduledeadline+ "  deadlineInfomodule  " + this.deadlineInfomodule);
				if(this.getHost().getUtilizationOfCpu()>= 0.30) { //if (this.getHost().getPower()>= 100) {	//if (getEnergyConsumption()> 5000) {					
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();								
					for(AppModule myvm : this.Appmoduledeadline.keySet()) {
						//System.out.println("=================================");
						//System.out.println("try to Migrate " +myvm.getName()+ " out of mobile "+ this.getId()+ " "+this.getName()+ " to "+ PDev.getName());						
						if (this.Executing==true) {
							//System.out.println("executing");
							mig=false;
							break;
							}
						for(Vm vm:  this.getHost().getVmList()) {
							//System.out.println(this.getName()+ vm.getId()+ ((AppModule)vm).getName() +" AllocatedMipsForVm "+this.getHost().getAllocatedMipsForVm(vm)+" CloudletScheduler CurrentRequestedMips  "+vm.getCloudletScheduler().getCurrentRequestedMips());
						}
						
						try {
						myvm.setInMigration(true);	
						Map<String, Application> myapplicationMap= applicationMap;						
						Application app = (Application)myapplicationMap.get("ECG");
						sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
											
						Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
						migrate.put("vm", (Vm)myvm);
						migrate.put("host",PHost);
						migrate.put("datacenter",PHost.getDatacenter());						
						//System.out.println(this.getName()+" datacenterid " +this.getId()+" hostid "+this.getHost().getId()+ " will migrate vm "+ myvm.getId()+ myvm.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
						//System.out.println(this.getName()+ " will migrate vm "+ myvm.getId()+ myvm.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
						sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);
						
						sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);
						// code to update number of instances for each module at the dept  //////////////						
						Object[] data = new Object[3];
						data[0] =(Vm)myvm;
						data[1]= this.getId(); 
						data[2] = 1;	//System.out.println(this.getName()+incomingOperator+ data[0]+data[1]);
						sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();
					}
					catch (Exception e)
					{
						//System.out.println("========================"); System.out.println("migration interupted");System.out.println("========================");
					}							
						migrated = new Pair<Integer, Vm>(this.getId(), (Vm)myvm);

						this.appToModulesMap.get("ECG").remove(((AppModule)migrated.getValue()).getName());
						this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
						this.getVmList().remove(migrated.getValue());
						
						String modnam = ((AppModule)migrated.getValue()).getName();
						Pair<Integer,String>migratednam= new Pair<Integer,String>(migrated.getKey(), modnam);
						this.appToModulesMapUsers.get("ECG").remove(migratednam);
						System.out.println(this.getName()+" CPU utilization "+this.getHost().getUtilizationOfCpu()+ " migrated " + myvm.getName());//" appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);
						
						this.Appmoduledeadline.remove(myvm);
						mig=false;
						break;					
						}						
					}// end mobile device   
			}// end default 
		}// end switch case		
	}
/////////////////////////////////////////////////////////////////////////
	/*
	private void checkDevicePerformanceEdgeward2() {
		// TODO Auto-generated method stub
		//System.out.println("fogdevice=========checkDevicePerformanceEdgeward============="+this.getName() + "  "+ this.getId()+" utilizationofcpu "+ this.getHost().getUtilizationOfCpu());//+ " host "+  this.getHost().getId() this.getHost().getPower()+ " utilization "+ this.getHost().getUtilizationOfCpuMips()+this.getHost().getUtilizationOfCpu()+ " getEnergyConsumption()" + getEnergyConsumption());
		FogDevice PDev= null; PowerHost PHost= null; Pair<Integer, Vm> migrated = new Pair<Integer, Vm>(null,null);
		switch (this.getName()) { 
			case "cloud" :{  
				//System.out.println("cloud"+ this.getName()+ this.getHost().getUtilizationOfCpu());
				break;
			}
			case "proxy-server" :{	
				if(this.getHost().getUtilizationOfCpu()> 0.90 && (this.moduledeadline.size()!=0)) {
					System.out.println(this.getHost().getUtilizationOfCpu());
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();
					
					AppModule myvm = getModuleByName("Monitoring");
					Map<String, Application> myapplicationMap= applicationMap;						
					Application app = (Application)myapplicationMap.get("ECG");					
					sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
					sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);					
					Object[] data = new Object[3];
					data[0] =(Vm)myvm;
					data[1]= 0; // mobileid of the module
					data[2] = 1;			
					sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();					
					
					//migrated = new Pair<Integer, Vm>(0, myvm);
					System.out.println("Migrating "+ myvm.getName()+" from device " + this.getName()+"  to device " + PDev.getName());
					}				
				break;
				}
			case "d-"+ 0 :
			case "d-"+ 1 :
			case "d-"+ 2 :{				
				System.out.println(this.getName()+ " "+this.getHost().getUtilizationOfCpu()+" moduledeadline " + this.moduledeadline);
				//System.out.println("vmlist " + this.getHost().getVmList());
				if((this.getHost().getUtilizationOfCpu()> 0.90) && (this.moduledeadline.size()!=0)) {										
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());						
					PHost=  PDev.getHost();
					//System.out.println(this.getName()+ " "+this.getHost().getUtilizationOfCpu()+ " proxy utilization "+ PHost.getUtilizationOfCpu());
					double PHostUtilizationOfCpu = PHost.getUtilizationOfCpu();
					if(PHostUtilizationOfCpu > 0.70) { //70 + this.getHost().getAllocatedMipsForVm(vm);							
						PDev= (FogDevice)CloudSim.getEntity("cloud");
						PHost=  PDev.getHost();
						System.out.println(this.getName()+ " "+ placement +" Proxy-server is full, migrating modules to "+ PDev.getName());
						}
					
					Map<String, Application> myapplicationMap= applicationMap;						
					Application app = (Application)myapplicationMap.get("ECG");
					sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
					
					Map<String, Double> dummoduledeadline= this.moduledeadline;
					List<String> dumappToModulesMap= this.appToModulesMap.get("ECG");
					try {
					for(String vm : dummoduledeadline.keySet()) {	//	map< appmodulename, double deadline> moduledeadline
						int i=0;
						for(String mod: dumappToModulesMap) { //Map<String, List<String>> appToModulesMap;							
							if(mod.equals(vm)) {
								AppModule myvm= getModuleByName(mod);
								i++;  // number of instance s																		
								//System.out.println(i+ "Migrating " + mod + " out of "+ this.getId()+ " to "+ PDev.getName());																					
		
								Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
								migrate.put("vm", myvm);
								migrate.put("host",PHost);
								migrate.put("datacenter",PHost.getDatacenter());
								//System.out.println(this.getName()+" datacenterid " +this.getId()+" hostid "+this.getHost().getId()+ " will migrate vm "+ myvm.getId()+ MOD.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
								//sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);														
								sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);																
																							
								// code to calculate number of instances  //////////////
								Object[] data = new Object[3];
								data[0] =myvm;
								data[1]= 0; // mobileid of the module
								data[2] = 1; // myvm.getNumInstances();
								//System.out.println("number of instances of " +myvm+ "  "+  data[2]);
								sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();
																								
								this.appToModulesMap.remove(mod);								
								this.getVmAllocationPolicy().deallocateHostForVm(myvm);
								this.getVmList().remove(myvm);
								System.out.println("Migrating module "+ myvm.getName() +" out of device " +this.getName()+ " to device " + PDev.getName()+ "  "+placement );
								//System.out.println("vm moduledeadline "+ vm + "mod apptomodulesmap " + mod +" "+  i);						
							}						
						}
						this.moduledeadline.remove(vm);
						break;
						}					
					}
					catch (Exception e) {
						e.printStackTrace();
						System.out.println("Unwanted errors happen");
					}
					}
			} // end dept
			default:{  			//mobile id  case 6:{     getUtilizationOfCpu() > 0.6666
				//System.out.println("mobile "+ this.getName()+this.appToModulesMap);	//System.out.println("mobile "+ this.getName()+" moduledeadline "+ this.moduledeadline+ "  deadlineInfomodule  " + this.deadlineInfomodule);
				if(this.getName().startsWith("m") && this.getHost().getUtilizationOfCpu()> 0.8000) { //if (this.getHost().getPower()>= 100) {	//if (getEnergyConsumption()> 5000) {			
					PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
					PHost=  PDev.getHost();
					//System.out.println("tooot  "+ PDev.getName());
					for(AppModule myvm : this.Appmoduledeadline.keySet()) {	
						//System.out.println("Migrating " +myvm.getName()+ " out of mobile "+ this.getId()+ " "+this.getName()+ " to "+ PDev.getName());					
						Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")
						migrate.put("vm", myvm);
						migrate.put("host",PHost);
						migrate.put("datacenter",PHost.getDatacenter());
						//System.out.println(this.getName()+" datacenterid " +this.getId()+" hostid "+this.getHost().getId()+ " will migrate vm "+ myvm.getId()+ MOD.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
						sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);
						
						Map<String, Application> myapplicationMap= applicationMap;						
						Application app = (Application)myapplicationMap.get("ECG");
						sendNow(PDev.getId(), FogEvents.APP_SUBMIT, app); //calls 	processAppSubmit(ev); Application app = (Application)ev.getData();
						
						sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);
						
						// code to update number of instances for each module at the dept  //////////////						
							Object[] data = new Object[3];
							data[0] =myvm;
							data[1]= 0;
							data[2] = 1;	//System.out.println(this.getName()+incomingOperator+ data[0]+data[1]);
							sendNow(PDev.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();
							
							migrated = new Pair<Integer, Vm>(this.getId(), myvm);
							this.Appmoduledeadline.remove(myvm);
							System.out.println("tooot  5  ");		
							
							break;					
						}						
					}// end mobile device
			}// end default 
			if(this.getName().startsWith("m")&& migrated.getKey()!= null) {
				String remod = ((AppModule)migrated.getValue()).getName();
				this.appToModulesMap.get("ECG").remove(remod);
				this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());			
				this.getVmList().remove(migrated.getValue());
			}
		}// end switch case
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	if(migrated.getKey()!= null && this.getName().startsWith("m")) {			//Pair<Integer, Vm> migrated	 
		this.appToModulesMap.get("ECG").remove(((AppModule)migrated.getValue()).getName());
		this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
		this.getVmList().remove(migrated.getValue());
		
		String modnam = ((AppModule)migrated.getValue()).getName();
		Pair<Integer,String>migratednam= new Pair<Integer,String>(migrated.getKey(), modnam);
		this.appToModulesMapUsers.get("ECG").remove(migratednam);
		System.out.println(this.getName()+ " migrated " + migrated +" appToModulesMap "+ this.appToModulesMap+ " appToModulesMapUsers "+this.appToModulesMapUsers);					
	}
	*/
	
	/*	List<Vm> cleanvm = new ArrayList<Vm>();
	for(Vm a : this.getHost().getVmList()) {
		for(Vm b : this.getHost().getVmList()) {
			if(a.getId()==b.getId()&& (!cleanvm.contains(a))) {						
				cleanvm.add(a);									
		}
	}
	}
	System.out.println ("cleanvm " + cleanvm.size()+  " " + cleanvm); //((AppModule)a).getName() + "  "
	if (this.Executing== true) {
		System.out.println ("executing");
		break;
	}
	else {
		for (Vm vm:cleanvm) {
		//System.out.println(" vm  " + vm);
			this.getVmAllocationPolicy().deallocateHostForVm(vm);
			this.getVmList().remove(vm);
		}
	}
	System.out.println(this.getName()+ " after cleaning vmlist size "+this.getHost().getVmList().size()+"appToModulesMapUsers "+ this.appToModulesMapUsers.get("ECG").size()+  this.appToModulesMapUsers.get("ECG"));
	System.out.println();
	*/
		/*if(migrated.getKey()!= null) {			//Pair<Integer, Vm> migrated			
			String remod = ((AppModule)migrated.getValue()).getName();
			this.moduledeadline.remove(remod);			
			if((this.getName().startsWith("d-"))||(this.getName().startsWith("proxy-server"))){
				System.out.println(this.getName()+ "  migrated mod " + remod +" vmlist "+  this.getVmList().size());
				for (int i=0; i<migrated.getKey(); i++) {			// remove all instances
					this.appToModulesMap.get("ECG").remove(remod);
					this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
					this.getVmList().remove(migrated.getValue());
				}
				/*
				System.out.println(this.getName() + " befor vmlist size " 	+ this.getVmList().size());
				System.out.println("vmlist  " + this.getVmList());
				List<Vm> myrevm = new ArrayList<Vm>();
				for( Vm revm: this.getVmList()) {					
					if (((AppModule)revm).getName().equals(remod)) {
						myrevm.add(revm);
						}
					}
				for(Vm revm:myrevm) {
					this.getVmAllocationPolicy().deallocateHostForVm(revm);
					this.getVmList().remove(revm);
				}				
				System.out.println(this.getVmList().size()+ " " + this.getVmList()) ;
				
			}
			else {				// mobile remove one instance 
				this.appToModulesMap.get("ECG").remove(remod);
				this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
				this.getVmList().remove(migrated.getValue());			
			}			
			
			//System.out.println(this.getName() + " after remove vmlist size " 	+ this.getVmList().size());
			//System.out.println("vmlist  " + this.getVmList());
			System.out.println(this.getName()+ "  appToModulesMap" + this.appToModulesMap.get("ECG") + "  " + remod + " moduledeadline "+ moduledeadline);
			System.out.println();
			*/
		//}
	
////////////////////////////////////////////////////////////////////////////////////////////
	//for(Pair<Integer,String> p: this.appToModulesMapUsers.get("ECG")){
	//if(p.getKey()==9) { continue;} //if(id==9) { //"m-0-1" //System.out.println("m-0-1 fixed on dept ");
	
	//Pair<Integer, String> devmod = new Pair<Integer, String>( migrated.getKey(),((AppModule)migrated.getValue()).getName());
	/*	Pair<Integer,Vm> migrated= checkDevicePerformance();  // userid, module
	if(migrated!= null) {	//for (PowerHost host : this.<PowerHost> getHostList()) { //System.out.println("return migrated  " +((AppModule)migrated).getName());	//for(int id: idOfEndDevices) {		if(id==this.getId()) {
		//System.out.println("return migrated idofenddevice " + migrated.getKey() +this.getName()+ this.getId() + " module "+ ((AppModule)migrated.getValue()).getName());		  	
		
		this.getVmAllocationPolicy().deallocateHostForVm(migrated.getValue());
		this.getVmList().remove(migrated.getValue());
		
		//Map<String, List<String>> appToModulesMap;  //appname, list of modulenames
		//System.out.println("befor rmigration this.appToModulesMap " + this.appToModulesMap);
		this.appToModulesMap.get("ECG").remove(((AppModule)migrated.getValue()).getName());				
		
		//System.out.println("appToModulesMapUsers " +appToModulesMapUsers);		
		Pair<Integer, String> devmod = new Pair<Integer, String>( migrated.getKey(),((AppModule)migrated.getValue()).getName());
		this.appToModulesMapUsers.get("ECG").remove(devmod);						
		//System.out.println("after remove usermod appToModulesMapUsers " +this.appToModulesMapUsers);
		
			/*for(Pair<Integer, String> p : this.appToModulesMapUsers.get("ECG")) {					
				if((p.getKey()==migrated.getKey())&&(p.getValue()==((AppModule)migrated.getValue()).getName())) {
					System.out.println("befor migration appToModulesMapUsersMob " +appToModulesMapUsers);
					appToModulesMapUsers.get("ECG").remove(p);
					System.out.println("after migration remove usermod from mob appToModulesMapUsersMob " +appToModulesMapUsers);
				}			
			} */
//		}
	//Map<String, Object> migrate = new HashMap<String, Object>(); //data is a Map<String, Object>")					
	//migrate.put("vm", (Vm)myvm);
	//migrate.put("host",PHost);
	//migrate.put("datacenter",PHost.getDatacenter());
	//System.out.println(this.getName()+" datacenterid " +this.getId()+" hostid "+this.getHost().getId()+ " will migrate vm "+ myvm.getId()+ MOD.getName()+ " to host "+ PHost.getId()+" datacenter "+ PHost.getDatacenter().getName() + " migrate[] = "+migrate);
	//sendNow(this.getId(),FogEvents.RELEASE_OPERATOR, migrate); // calls	processOperatorRelease(ev);
	//if (MOD.getName()=="Client") {
		//break; //return null;						
	//} 
	/*		//System.out.println("returened mobmodu" + miguservm);
	 			Integer idOfEndDevice = this.getId();
						Pair<Integer, String> devmod = new Pair<Integer, String>(idOfEndDevice, MOD.getName());
						appToModulesMapUsers.get("ECG").add(devmod);						
						System.out.println("after migration add usermod to dept appToModulesMapUsersDept " +appToModulesMapUsersDept);
						//if(appToModulesMapUsersMob.get("ECG").contains(idOfEndDevice))
						for(Pair<Integer, String> p : appToModulesMapUsersMob.get("ECG")) {							
							if((p.getKey()==idOfEndDevice)&&(p.getValue()==MOD.getName())) {
								System.out.println("befor migration appToModulesMapUsersMob " +appToModulesMapUsersMob);
								appToModulesMapUsersMob.get("ECG").remove(p);
								System.out.println("after migration remove usermod from mob appToModulesMapUsersMob " +appToModulesMapUsersMob);
								}
							System.out.println("hiiiiiiiiiiiiiiiiiiii");
							} */		
						//System.out.println("after migration remove usermod from mob appToModulesMapUsersMob " +appToModulesMapUsersMob);
						//System.out.println("returened idOfEndDevice, vm " + this.getId() + myvm.getName());
					//Pair<Integer, Vm> migrated = new Pair<Integer, Vm>(this.getId(), (Vm)myvm);
			//Map<String, List<String>> appToModulesMap;  //appname, list of modulenames
			//System.out.println("befor rmigration this.appToModulesMap " + this.appToModulesMap);
			//System.out.println("appToModulesMapUsers " +appToModulesMapUsers);		
			 	
		//System.out.println(this.getName()+ "  deadlineinfo " +this.deadlineInfo);
		//System.out.println("Appmoduledeadline" +this.Appmoduledeadline);
		//System.out.println("moduledeadline" +this.moduledeadline);
		//System.out.println("deadlineInfomodule" +this.deadlineInfomodule);
		//System.out.println(this.getName()+ " appToModulesMapUsers" +this.appToModulesMapUsers);
		
		//List<Pair<Integer,String>> myusersECG = this.appToModulesMapUsers.get("ECG");
	/*
	Map<Vm,Integer> VmListPrty = new HashMap<Vm,Integer>();  // mobile id need deadline double
	for (Vm vm : this.getHost().getVmList()) {
		MOD= (AppModule)vm;
		VmListPrty.put(MOD, MOD.getPrty());
	}
	System.out.println("mobile "+ this.getName() +" modListPrty " +VmListPrty);
	Map<Vm, Integer> prioVmListPrty = VmListPrty.entrySet()
         .stream().sorted(Map.Entry.comparingByValue())
         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	System.out.println("mobile "+ this.getName() +" prioVmListPrty " + prioVmListPrty );
	for(Vm myvm : prioVmListPrty.keySet()) {
		MOD=(AppModule)myvm;
		System.out.println("migrating " +MOD.getName()+ " out of mobile "+ this.getId());
		if (MOD.getName()=="Client") {
			break; //return null;						
		}
		*/
	//Log.printLine("VmListPrty "+ ((AppModule)vm).getName()+ " prty is "+ ((AppModule)vm).getPrty());						
	//MOD = (AppModule)getHost().getVmList().get(0);
	//for(Vm vm :VmListPrty.keySet())
					//Log.printLine("VmListPrty "+ ((AppModule)vm).getName()+ " prty is "+ ((AppModule)vm).getPrty());				
					//Log.printLine("prioVmListPrty "+ ((AppModule)vm).getName()+ " prty is "+ ((AppModule)vm).getPrty());
				//Vm myvm = Map.Entry. prioVmListPrty;
	//if(PDev.getVmAllocationPolicy().allocateHostForVm(MOD,PHost)) {
	//this.getVmAllocationPolicy().deallocateHostForVm(myvm);					
	//this.getVmList().remove(myvm);
	 //FogEvents.APP_SUBMIT // calls 	processAppSubmit(ev);
	//	Application app = (Application)ev.getData();
	//protected Map<String, Application> applicationMap;
	//System.out.println("applicationMap  " +applicationMap);
	//sendNow(PDev.getId(),FogEvents.APP_SUBMIT, applicationMap.get(0));
	//sendNow(PDev.getId(), FogEvents.LAUNCH_MODULE, myvm); //calls processModuleArrival(ev);
	//System.out.println("vmhost "+ myvm.getHost() +"hostvmlist "+ this.getVmList());
	//PDev.getVmAllocationPolicy().allocateHostForVm(MOD,PHost);					
	//PHost.addMigratingInVm(myvm);  //myvm.setInMigration(true);
	//if(!PHost.getVmList().contains(myvm)) {
		//PHost.getVmList().add(myvm);											
	//}					
	//System.out.println("PHost.getVmList() "+PHost.getVmList());
	//PDev.updateAllocatedMips(MOD.getName());
	//this.setSchedulingInterval(schedulingInterval);//Log.printLine("VM #" + myvm.getId() + " has been deallocated from host #" + host.getId()+ " device "+ this.getName()+ " try to allocate VM # " + myvm.getId() + " to host #" + PHost.getId()+ " device "+ PDev.getName());									
	//Log.printLine(((AppModule)myvm).getName() + " has been migrated, Migration of VM  " +myvm.getId()+ " to Host  " + PHost.getId()+ " is completed" + CloudSim.clock());	
//////////////////////////////////end ragaa  checkDevicePerformance() /////////////////////////////////////
	
	protected void checkCloudletCompletion() {
		//System.out.println("fogdevice checkCloudletCompletion--------------------------");
		boolean cloudletCompleted = false;
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				//System.out.println(host.getDatacenter().getName()+" host"+ host.getId()+ "vm "+ vm.getId());
				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					//System.out.println(host.getDatacenter().getName()+" host"+ host.getId()+ "vm "+ vm.getId() + " finished cl  "+cl);
					if (cl != null) {						
						cloudletCompleted = true;
						Tuple tuple = (Tuple)cl;
						TimeKeeper.getInstance().tupleEndedExecution(tuple);						
						//TimeKeeper.getInstance().tupleEndedExecutioncloudlets(tuple);  // ragaanew //////////////
						
						TimeKeeper.getInstance().tupleEndedExecutionUser(tuple);  // ragaanew  calculate average cloudlet time for each user //////////////
						
						Application application = getApplicationMap().get(tuple.getAppId());
						Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
						//System.out.println(getName()+ "Completed execution of tuple "+tuple.getCloudletId()+" source "+tuple.getSrcModuleName()+" dest "+tuple.getDestModuleName()+"tuple module copy map"+tuple.getModuleCopyMap() + " mobile id "+tuple.getTPrty());
						List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
						//System.out.println("resultantTuples "+ resultantTuples);
						for(Tuple resTuple : resultantTuples){							
							resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));							
							resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
							//System.out.println(this.getName()+ " result tuple source " + resTuple.getSrcModuleName()+" des "+resTuple.getDestModuleName()+" length "+resTuple.getCloudletLength());
							//System.out.println(this.getName()+ " resut tuple ModuleCopyMap "+ resTuple.getModuleCopyMap());		
	///////////////////  ragaa corrected code to update the cloudlet length according to each user  ///////////////////////////////
							resTuple.setTPrty(tuple.getTPrty());  // double mobile id
							int mobid = (int)resTuple.getTPrty();  
							//String modnam= resTuple.getDestModuleName();											
							for(int a: this.additionalMipsInfo.keySet()) {
								if (a== mobid) {
									double x= this.additionalMipsInfo.get(a);										
									resTuple.setCloudletLength((long) x);
									break;
									}
								}
	/////////////////////////////////////////////////////////////////////////////
							//System.out.println(this.getName()+ " result tuple source " + resTuple.getSrcModuleName()+" des "+resTuple.getDestModuleName()+" new   length "+resTuple.getCloudletLength());
							updateTimingsOnSending(resTuple);
							sendToSelf(resTuple);
						}
						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
					}
				}
			}
		}
		if(cloudletCompleted) 
			updateAllocatedMips(null);		
	}
	/////////////////////////////////////////////////////////////////////
	///////////////////source code //////////////////////////////////////////
	/*
	 protected void checkCloudletCompletion() {
		//System.out.println("fogdevice checkCloudletCompletion--------------------------");
		boolean cloudletCompleted = false;
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				//System.out.println(host.getDatacenter().getName()+" host"+ host.getId()+ "vm "+ vm.getId());
				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					//System.out.println(host.getDatacenter().getName()+" host"+ host.getId()+ "vm "+ vm.getId() + " finished cl  "+cl);
					if (cl != null) {						
						cloudletCompleted = true;
						Tuple tuple = (Tuple)cl;
						TimeKeeper.getInstance().tupleEndedExecution(tuple);
						//TimeKeeper.getInstance().tupleEndedExecutioncloudlets(tuple);  // ragaa //////////////
						Application application = getApplicationMap().get(tuple.getAppId());
						Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
						//System.out.println(getName()+ "Completed execution of tuple "+tuple.getCloudletId()+"source "+tuple.getSourceModuleId()+" dest "+tuple.getDestModuleName()+"tuple module copy map"+tuple.getModuleCopyMap());
						List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
						//System.out.println("resultantTuples "+ resultantTuples);
						for(Tuple resTuple : resultantTuples){							
							resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
							//System.out.println("source " + resTuple.getSrcModuleName()+" des "+resTuple.getDestModuleName()+" ModuleCopyMap "+ resTuple.getModuleCopyMap());
							
							resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
							//System.out.println("ModuleCopyMap "+ resTuple.getModuleCopyMap());
							
							updateTimingsOnSending(resTuple);
							sendToSelf(resTuple);
						}
						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
					}
				}
			}
		}
		if(cloudletCompleted)
			updateAllocatedMips(null);
	} 
	*/
	/////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////
	protected void updateTimingsOnSending(Tuple resTuple) {
		// TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE. 
		// WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
		String srcModule = resTuple.getSrcModuleName();
		String destModule = resTuple.getDestModuleName();
		//System.out.println();
		//System.out.println("updateTimingsOnSending------------------------------- srcmod "+ srcModule+ " destModule "+destModule);
		for(AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()){
			//System.out.println(loop.toString()+ loop.hasEdge(srcModule, destModule));
			if(loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)){				
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				resTuple.setActualTupleId(tupleId);
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
				
				//Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);
				//System.out.println("resultanttuplegotid "+getName()+ "SENDING"+ tupleId +"\tSrc:"+srcModule+"\tDest:"+destModule);				
			}
			//System.out.println(getName()+ "SENDING restuple.Actualid "+ resTuple.getActualTupleId() +"\tSrc:"+srcModule+"\tDest:"+destModule);
		}
	}

	protected int getChildIdWithRouteTo(int targetDeviceId){
		for(Integer childId : getChildrenIds()){
			if(targetDeviceId == childId)
				return childId;
			if(((FogDevice)CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
				return childId;
		}
		return -1;
	}
	
	protected int getChildIdForTuple(Tuple tuple){
		if(tuple.getDirection() == Tuple.ACTUATOR){
			int gatewayId = ((Actuator)CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
			return getChildIdWithRouteTo(gatewayId);
		}
		return -1;
	}
//************************************source code///////////////////	
/*	protected void updateAllocatedMips(String incomingOperator){
		getHost().getVmScheduler().deallocatePesForAllVms();		
		for(final Vm vm : getHost().getVmList()){
			if(vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule)vm).getName().equals(incomingOperator)){
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}else{
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add(0.0);}});
			}
		}		
		updateEnergyConsumption();
		
	} */
//////////////////////////////////////ragaa code //////////////////////////////////////////////////////////////
	protected void updateAllocatedMips(String incomingOperator){
		//System.out.println("updateAllocatedMips incomingOperator "+ incomingOperator+ "  VmList : " +this.getHost().getVmList());
		getHost().getVmScheduler().deallocatePesForAllVms();
		Vm myincomingvm= null;
		AppModule myAppModule = null;
		for(final Vm vm : getHost().getVmList()) {
			if (((AppModule)vm).getName().equals(incomingOperator)) {
				myincomingvm= vm;
				myAppModule= ((AppModule)myincomingvm);			
			}
		}
		if(incomingOperator!=null)
			//System.out.println("updateAllocatedMips userid "+  myAppModule.getPrty()+ " "+  myAppModule.getName()+ " "+myAppModule.getUid() +"  " + myAppModule.getMips());	
		
		for(final Vm vm : getHost().getVmList()){
			if(vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule)vm).getName().equals(incomingOperator)){
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				//{add((double) vm.getMips());}});
				{add((double) getHost().getTotalMips());}});
				//System.out.println(this.getName()+ " allocate vm "+vm.getId()+ " "+ ((AppModule)vm).getName()+ " totalmips"+ getHost().getTotalMips()+ "   ");
			}else{
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add(0.0);}});
				//System.out.print("allocate "+ ((AppModule)vm).getName()+ " totalmips"+ 0 + "  ");
			}
			//System.out.println();
		}
		
		updateEnergyConsumption();		
	}
	///////////////////////////////////////////////////////////////ragaanew add updateallocatedmips with vm as an input////
	protected void MYupdateAllocatedMips(AppModule module){
		//System.out.println("updateAllocatedMips incomingOperator "+ incomingOperator+ "  VmList : " +this.getHost().getVmList());
		getHost().getVmScheduler().deallocatePesForAllVms();
		Vm myincomingvm= null;
		//if(incomingOperator!=null)
			//System.out.println("updateAllocatedMips userid "+  myAppModule.getPrty()+ " "+  myAppModule.getName()+ " "+myAppModule.getUid() +"  " + myAppModule.getMips());	
		
		for(final Vm vm : getHost().getVmList()){
			if(vm.getCloudletScheduler().runningCloudlets() > 0 || vm.getId()== module.getId()){
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) vm.getMips());}});
				//{add((double) getHost().getTotalMips());}});
				//System.out.println(this.getName()+ " allocate vm "+vm.getId()+ " "+ ((AppModule)vm).getName()+ " totalmips"+ getHost().getTotalMips()+ "   ");
			}else{
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add(0.0);}});
				//System.out.print("allocate "+ ((AppModule)vm).getName()+ " totalmips"+ 0 + "  ");
			}
			//System.out.println();
		}
		
		updateEnergyConsumption();		
	}
	////////////////end ragaanew updateallocatedmips //////////////////////////////////////////////////////////
	/*
	//updateModuleInstanceCount(SimEvent ev)	ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
	//public ModuleLaunchConfig(AppModule module, int instanceCount){
	//if(getName().equals("d-0") && tuple.getTupleType().equals("SENSOR_DATA")){
	//if(getName().equals("d-0") && tuple.getTupleType().equals("FILTERED_DATA")){
	//AppModule dest= getModuleByName(tuple.getDestModuleName());		
	//Map<AppModule, Integer> modlunch = new HashMap<AppModule, Integer>();
	//modlunch.put(myAppModule, ++finst); //modlunch.put(myAppModule, ++minst);
	Object[] data = new Object[2];
	data[0] = myAppModule;				
	if(getName().equals("d-0") && incomingOperator=="Filtering"){
		data[1] = ++finst;
		//System.out.println(this.getName()+incomingOperator+ data[0]+data[1]);
		sendNow(this.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();			
	}
	if(getName().equals("d-0") && incomingOperator=="Monitoring"){
		data[1] = ++minst;
		//System.out.println(this.getName()+incomingOperator+ data);
		sendNow(this.getId(),FogEvents.LAUNCH_MODULE_INSTANCE,data);  // calls updateModuleInstanceCount();			
	}*/
	//System.out.println("myincomingvm "+ myincomingvm + " myAppModule" +myAppModule);		
	//if (this.getHost().getVmsMigratingIn().contains(myincomingvm)) {
		//System.out.println("migrated " + incomingOperator);
		//myincomingvm.setInMigration(true);}
	//System.out.println(" running cloudlets " + myincomingvm.getCloudletScheduler().runningCloudlets());
	//System.out.println("updateAllocatedMips host "+ this.getHost().getId()+ "  VmList : " +this.getHost().getVmList());
////////////////////////////////////////////////////////////////////////////////////////////////////
	private void updateEnergyConsumption() {
		double totalMipsAllocated = 0;
		//System.out.println("fogdevice updateEnergyConsumption "+this.getName()+ " host "+getHost().getId()); //+ "  "+ getHost().getVmList()
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
					.getAllocatedMipsForVm(operator));			
			totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
			//System.out.println(operator.getName() +"   totalMipsAllocated to the host  "+ totalMipsAllocated);
		}
		double timeNow = CloudSim.clock();
		double currentEnergyConsumption = getEnergyConsumption();
		double newEnergyConsumption = currentEnergyConsumption + (timeNow-lastUtilizationUpdateTime)*getHost().getPowerModel().getPower(lastUtilization);
		setEnergyConsumption(newEnergyConsumption);
		//System.out.println("getEnergyConsumption "+getEnergyConsumption());
	
		/*if(getName().equals("d-0")){
			System.out.println("------------------------");
			System.out.println("Utilization = "+lastUtilization);
			System.out.println("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			System.out.println(timeNow-lastUtilizationUpdateTime);
		}*/
		
		double currentCost = getTotalCost();
		double newcost = currentCost + (timeNow-lastUtilizationUpdateTime)*getRatePerMips()*lastUtilization*getHost().getTotalMips();
		setTotalCost(newcost);
		
		lastUtilization = Math.min(1, totalMipsAllocated/getHost().getTotalMips());
		lastUtilizationUpdateTime = timeNow;
		
	}
	//setEnergyConsumption(getHost().getPowerModel().getStaticPower());  // ragaa update it at foglinearpowermodel and powermodel classes
	//System.out.println("static power EnergyConsumption "+getEnergyConsumption());
//////////////////////////////////////////////////////////////////////////////////////////////////////
	protected void processAppSubmit(SimEvent ev) {
		Application app = (Application)ev.getData();
		applicationMap.put(app.getAppId(), app);
	}

	protected void addChild(int childId){
		if(CloudSim.getEntityName(childId).toLowerCase().contains("sensor"))
			return;
		if(!getChildrenIds().contains(childId) && childId != getId())
			getChildrenIds().add(childId);
		if(!getChildToOperatorsMap().containsKey(childId))
			getChildToOperatorsMap().put(childId, new ArrayList<String>());
	}
	
	protected void updateCloudTraffic(){
		int time = (int)CloudSim.clock()/1000;
		if(!cloudTrafficMap.containsKey(time))
			cloudTrafficMap.put(time, 0);
		cloudTrafficMap.put(time, cloudTrafficMap.get(time)+1);
	}
	
	protected void sendTupleToActuator(Tuple tuple){
		/*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
		for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			String actuatorType = ((Actuator)CloudSim.getEntity(actuatorId)).getActuatorType();
			if(tuple.getDestModuleName().equals(actuatorType)){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		for(int childId : getChildrenIds()){
			sendDown(tuple, childId);
		}
	}

	int numClients=0;
/////////////////////////////////////////////////////////////////////////////////
///////////////////////////update to support migrated vm ragaa///////////////////////
	
	protected void processTupleArrival(SimEvent ev){			
		Tuple tuple = (Tuple)ev.getData();				 
		//System.out.println("fogdevice processTupleArrival_______________________ ev "+ ev+ " evsource "+ev.getSource()+ " evdest "+ ev.getDestination()+ " evdata "+ev.getData()); 

		if(getName().equals("cloud")){
			updateCloudTraffic();
		}		
		/*if(getName().equals("d-0") && tuple.getTupleType().equals("_SENSOR")){
			System.out.println(++numClients);
		}*/
		Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+
		CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
		
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		
		if(FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())){
		}
		
		if(tuple.getDirection() == Tuple.ACTUATOR){
			sendTupleToActuator(tuple);
			return;
		}
		
		if(getHost().getVmList().size() > 0){
			final AppModule operator = (AppModule)getHost().getVmList().get(0);
			//System.out.println(this.getName()+ "host " + getHost().getId()+ operator.getName()+ " vmlist "+ getHost().getVmList());
			if(CloudSim.clock() > 0){
				getHost().getVmScheduler().deallocatePesForVm(operator);			
	/////ragaanew assign various resources to vms priority time shared scheduling 
				if(this.placement.equals("Enhanced Latency Differentiated Module Placement"))
				getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) operator.getMips());}});  
				//{add((double) getHost().getTotalMips());}});
				else
					getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
						protected static final long serialVersionUID = 1L;
					{add((double) getHost().getTotalMips());}});
				// end ragaanew 
			}
		}		
		
		if(getName().equals("cloud") && tuple.getDestModuleName()==null){
			sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
		}
		
		if(appToModulesMap.containsKey(tuple.getAppId())){
		//if(appToModulesMapUsers.containsKey(tuple.getAppId())){
			//System.out.println(this.getName()+ "appToModulesMap "+ appToModulesMap);
			if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())){
			//if(appToModulesMapUsers.get(tuple.getAppId()).get(tuple.getSourceDeviceId()).contains(tuple.getDestModuleName())){
				//System.out.println("tuple.getDestModuleName "+tuple.getDestModuleName());
				int vmId = -1;
				for(Vm vm : getHost().getVmList()){  // getDatacenter().getHost().getVmList()){					
					if(((AppModule)vm).getName().equals(tuple.getDestModuleName())) {
						//System.out.println("tuple.getDestModuleName "+tuple.getDestModuleName()+" destination module vmid "+vm.getId()+((AppModule)vm).getName());
						vmId = vm.getId();						
					}				
				}
				//System.out.println(this.getName()+ " vmId "+ vmId+" tupleDest "+tuple.getDestModuleName()+
					//" dest vmId "+vmId+" ModuleCopyMap"+ tuple.getModuleCopyMap());
				if(vmId < 0
						|| (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
								tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId )){
					//System.out.println("hiiiii am i here ??");
					return;
				}
				
				tuple.setVmId(vmId);
				//Logger.error(getName(), "Executing tuple for operator " + moduleName);

				//System.out.println(getName()+ " Executing tuple for dest operator " + tuple.getDestModuleName()+" vmid "+vmId+  " tuple "+ tuple);
				
				updateTimingsOnReceipt(tuple);				
				executeTuple(ev, tuple.getDestModuleName());				
				//System.out.println(getName()+ " finished Executing tuple for dest operator " + tuple.getDestModuleName()+" vmid "+vmId+  " tuple "+ tuple);
			}// else if destination module not exist on application to module map
			
			else if(tuple.getDestModuleName()!=null){
				if(tuple.getDirection() == Tuple.UP)
					sendUp(tuple);
				else if(tuple.getDirection() == Tuple.DOWN){
					for(int childId : getChildrenIds())
						sendDown(tuple, childId);
				}
			}else{
			//	System.out.println("destmodule= null??");
				sendUp(tuple);
			}
		}// else if apptomodule map does not contain the application
		else{
			if(tuple.getDirection() == Tuple.UP)				
				sendUp(tuple);
			
			else if(tuple.getDirection() == Tuple.DOWN){
				for(int childId : getChildrenIds())
					sendDown(tuple, childId);
				}
			}
}
/////////////////////////////////////////////////////////////////////////////////
	/*
	 		/*Integer des= ev.getDestination();
		Integer src =ev.getSource();
		String dev= CloudSim.getEntityName(des);		

		
		//System.out.println("src" + src+ "des"+ des+ "dev"+ dev+	idOfEndDevices.contains(des));

		//System.out.println(getName()+ " Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+
			//CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
		//if(getName().startsWith("m") && deviceById.containsValue(CloudSim.getEntityName(ev.getDestination()))){ //&& tuple.getTupleType().equals("_SENSOR")){
		/*
		if(getName().startsWith("m") && idOfEndDevices.contains(des)){ //&& tuple.getTupleType().equals("_SENSOR")){
			Pair<Integer, String> p= new Pair<Integer, String>(des,((AppModule)ev.getData()).getName());
			appToModulesMapUsers.get("ECG").add(p);
			System.out.println(this.getName()+ " appToModulesMapUsers "+ appToModulesMapUsers);
			
			 //deadlineInfomodule.put(this.getId(), value)
		}
		
		//if(getName().startsWith("d") &&&& deviceById.containsKey(des)&& deviceById.containsValue(CloudSim.getEntityName(ev.getDestination()))){ //&& tuple.getTupleType().equals("_SENSOR")){
			if(getName().startsWith("d") && tuple.getTupleType().equals("_SENSOR")){
				
				String mod= ((AppModule)ev.getData()).getName();
				System.out.println(this.getName()+ mod);
				Pair<Integer, String> p= new Pair<Integer, String>(src,mod);
				if(!this.appToModulesMapUsers.get("ECG").contains(p))
					this.appToModulesMapUsers.get("ECG").add(p);
				System.out.println(this.getName()+ " appToModulesMapUsers "+ appToModulesMapUsers);
			}
			if(getName().startsWith("proxy")&& tuple.getTupleType().equals("ECG_REPORT")){
				String mod= ((AppModule)ev.getData()).getName();
				Pair<Integer, String> p= new Pair<Integer, String>(src,mod);
				if(!this.appToModulesMapUsers.get("ECG").contains(p))
					appToModulesMapUsers.get("ECG").add(p);
				System.out.println(this.getName()+ " appToModulesMapUsers "+ appToModulesMapUsers);
			}*/
			//System.out.println(this.getName()+ "processtuplearrival appToModulesMapUsers "+ appToModulesMapUsers);
		//}
			/*///////////ragaa code to fill apptomodulesmapusers		
		if(getName().equals("d-0") && !appToModulesMapUsersDept.containsKey(tuple.getAppId()))
			appToModulesMapUsers.put(this.getActiveApplications().get(0), new HashMap<Integer,List<String>>());
		System.out.println("appToModulesMapUsers "+ appToModulesMapUsers);
		if (getName().equals("d-0") && idOfEndDevices.contains(tuple.getSourceDeviceId())) {
			if(getName().equals("d-0") &&!appToModulesMapUsers.get(tuple.getAppId()).containsKey(tuple.getSourceDeviceId()))
				appToModulesMapUsers.get(tuple.getAppId()).put(tuple.getSourceDeviceId(), new ArrayList<String>());
			appToModulesMapUsers.get(tuple.getAppId()).get(tuple.getSourceDeviceId()).add(tuple.getDestModuleName());
		}
		System.out.println("appToModulesMapUsers "+appToModulesMapUsers);

	 if () {  //vm migrated  module.getHost().getId()!= this.getId()			 
			int type = CloudSimTags.CLOUDLET_MOVE_ACK;
			int[] array = new int[5];			
			array[0]= (int)tuple.getCloudletId(); //int cloudletId;
			array[1]= (int)tuple.getUserId();	 //int userId ;
			array[2]= (int)tuple.getVmId(); 	//int vmId;
			array[3]= (int)tuple.getVmId();    // int vmDestId;
			array[4]= (int)module.getHost().getId();   //int destId;		
			System.out.println("fogdevice process cloudletmove " +"module host"+module.getHost().getId()+ "this id "+this.getId());
			System.out.println("array"+ tuple.getCloudletId()+ " ?? " + array[0]+array[1]+array[2]+array[3]+array[4]);
			processCloudletMove(array, type);
		}		
	 */
/////source code////////////////////////////////////////////////////////////////////////ragaa
///////source code////////////////////////////////////////////////////////////////////////ragaa
	/*protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		//System.out.println("fogdevice processTupleArrival========================");
		System.out.println("fogdevice processTupleArrival_______________________ ev "+ ev);
		System.out.println(getName()+  "Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+
				CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
		
		if(getName().equals("cloud")){
			updateCloudTraffic();
		}
		
		///*if(getName().equals("d-0") && tuple.getTupleType().equals("_SENSOR")){
			//System.out.println(++numClients);
		//}
		Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+
		CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
		
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
		
		if(FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())){
		}
		
		if(tuple.getDirection() == Tuple.ACTUATOR){
			sendTupleToActuator(tuple);
			return;
		}
		
		if(getHost().getVmList().size() > 0){
			final AppModule operator = (AppModule)getHost().getVmList().get(0);
			//System.out.println("processtuplearrival host " + getHost().getId()+ operator.getName()+ " vmlist "+ getHost().getVmList());
			if(CloudSim.clock() > 0){
				getHost().getVmScheduler().deallocatePesForVm(operator);
				getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}
		}		
		
		if(getName().equals("cloud") && tuple.getDestModuleName()==null){
			sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
		}
		
		if(appToModulesMap.containsKey(tuple.getAppId())){
			if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())){
				//System.out.println("tuple.getDestModuleName "+tuple.getDestModuleName());
				int vmId = -1;
				for(Vm vm : getHost().getVmList()){
					if(((AppModule)vm).getName().equals(tuple.getDestModuleName()))
						vmId = vm.getId();
				}
				if(vmId < 0
						|| (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
								tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId )){
					return;
				}
				tuple.setVmId(vmId);
				//Logger.error(getName(), "Executing tuple for operator " + moduleName);
				
				updateTimingsOnReceipt(tuple);
				//System.out.println(getName() + "processtuplearrival vmid "+vmId+  "tuple"+ tuple); //ragaa
				
				executeTuple(ev, tuple.getDestModuleName());
				
			}else if(tuple.getDestModuleName()!=null){
				if(tuple.getDirection() == Tuple.UP)
					sendUp(tuple);
				else if(tuple.getDirection() == Tuple.DOWN){
					for(int childId : getChildrenIds())
						sendDown(tuple, childId);
				}
			}else{
				//System.out.println("destmodule= null??");
				sendUp(tuple);
			}
		}else{
			if(tuple.getDirection() == Tuple.UP)				
				sendUp(tuple);
			
			else if(tuple.getDirection() == Tuple.DOWN){
				for(int childId : getChildrenIds())
					sendDown(tuple, childId);
			}
		}
	}
	//end source code/////////////////////////////////////////////////////////////////////////////////////*/
	protected void updateTimingsOnReceipt(Tuple tuple) {
		Application app = getApplicationMap().get(tuple.getAppId());
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName();		
		List<AppLoop> loops = app.getLoops();
		//System.out.println("tuple actual id "+tuple.getActualTupleId()+ " src "+tuple.getSrcModuleName() +" des " + tuple.getDestModuleName()+" tuple Prty "+ tuple.getTPrty()+ " tuple type "+tuple.getTupleType());
		Integer mobileid=this.getId();  		 //ragaa 			
		for(AppLoop loop : loops){
			if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){				
				Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
////////////////////////////////////average delay loop all users/////////////////////////////////////////////////////////
				if(startTime==null)
					break;
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
					TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
				}				
				double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
				int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
				double delay = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				//TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
				TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
				TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
				//System.out.println("tupleactualtupleid "+ tuple.getActualTupleId()+" tuplecloudletid "+ tuple.getCloudletId()+ " mobileid " + mobileid + " starttime "+ startTime+ "delay "+ delay+ "newaverage "+ newAverage);
				
/////////////////ragaanew////// average delay loop analytics user///////////////////////////////								
				//if(!app.getUserAnalytics().containsKey((int)tuple.getTPrty()))  //ragaanew
					//break;
				Map<Integer,Double> a= new HashMap<Integer, Double>();
				Map<Integer,Integer> b= new HashMap<Integer, Integer>();				
				a.put((int)tuple.getTPrty(),0.0);					
				b.put((int)tuple.getTPrty(),0);
				
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().put(loop.getLoopId(),a);
					TimeKeeper.getInstance().getLoopIdToCurrentNumUserAnalytics().put(loop.getLoopId(),b);
				}
				Map<Integer,Double> x= new HashMap<Integer, Double>();
				Map<Integer,Integer> y= new HashMap<Integer, Integer>();
				x=TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().get(loop.getLoopId());
				y=TimeKeeper.getInstance().getLoopIdToCurrentNumUserAnalytics().get(loop.getLoopId());								
				
				if(!x.containsKey((int)tuple.getTPrty())) 					
					x.put((int)tuple.getTPrty(),0.0);
				
				if(!y.containsKey((int)tuple.getTPrty())) 						
					y.put((int)tuple.getTPrty(),0);
				
				//System.out.println("sssssanalytics users "+ TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics() + "    "+TimeKeeper.getInstance().getLoopIdToCurrentNumUserAnalytics());
				//if (this.getName().startsWith("proxy")) {  //ragaanew  average analytics delay for a user
				double currentAverageUserAnalytics = TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().get(loop.getLoopId()).get((int)tuple.getTPrty());
				//System.out.println("sssssanalytics users  currentAverageUserAnalytics "+ currentAverageUserAnalytics);
				int currentCountUserAnalytics = TimeKeeper.getInstance().getLoopIdToCurrentNumUserAnalytics().get(loop.getLoopId()).get((int)tuple.getTPrty());
				double delayUserAnalytics = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				//System.out.println("sssssanalytics users  delayUserAnalytics "+ delayUserAnalytics);
				//TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				
				double newAverageUserAnalytics = (currentAverageUserAnalytics*currentCountUserAnalytics + delayUserAnalytics)/(currentCountUserAnalytics+1);
				//System.out.println("sssssanalytics users  newAverageUserAnalytics "+ newAverageUserAnalytics);
				TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().get(loop.getLoopId()).put((int)tuple.getTPrty(), newAverageUserAnalytics);
				TimeKeeper.getInstance().getLoopIdToCurrentNumUserAnalytics().get(loop.getLoopId()).put((int)tuple.getTPrty(), currentCountUserAnalytics+1);
				
				//System.out.println("sssssanalyticsfinal users "+ TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics() + "    "+TimeKeeper.getInstance().getLoopIdToCurrentNumUserAnalytics());
				//app.getUserAnalytics().get((int)tuple.getTPrty()).put(tuple.getTupleType(),newAverageUserAnalytics);
				//System.out.println("appanalyticsTimeUser "+ app.getUserAnalytics());
				//System.out.println("tupactualid "+ tuple.getActualTupleId()+" cloudletid "+ tuple.getCloudletId()+ " Mobid " + mobileid + " starttime "+ startTime+ " delayUser "+ delayUserAnalytics);
				//System.out.println("getCurrentAverageUserAnalytics "+TimeKeeper.getInstance().getCurrentAverageUserAnalytics() + " LoopIdToCurrentNumUserAnalytics "+TimeKeeper.getInstance().getCurrentNumUserAnalytics());					
			 
	/////////////end ragaanew  average delay loop analytics user ////////////////////////////////////////////////
///////////////////////////////////////ragaanew check if average delay loop analytics user exceed limit //////////////////////////////////////////////
				/* Map<Integer,List< Map<String,Double>>> getUsertupleTypeToAverageCpuTime()
				Map<Integer,Double> dumx;
				dumx=TimeKeeper.getInstance().getLoopIdToCurrentAverageUserAnalytics().get(loop.getLoopId());
				  for (Map.Entry<Integer,Double> entry : dumx.entrySet()) {  
			           if((entry.getValue()> 50)&& !(this.unsatisfiedUsers.contains(entry.getKey())))
			        	   this.unsatisfiedUsers.add(entry.getKey());			         
				  } */
//////////////////////////////////end ragaanew check users delay /////////////////////////////
	/////////////////////////////////average delay loop user/////////////////////////////////
				if (this.getName().startsWith("m")) {   //ragaa   
					if(!TimeKeeper.getInstance().getCurrentAverageUser().containsKey(mobileid)){
					TimeKeeper.getInstance().getCurrentAverageUser().put(mobileid, 0.0);
					TimeKeeper.getInstance().getCurrentNumUser().put(mobileid, 0);
					}           //ragaa 
				}
				if (this.getName().startsWith("m")) {  //ragaa 
					double currentAverageUser = TimeKeeper.getInstance().getCurrentAverageUser().get(loop.getLoopId());
					int currentCountUser = TimeKeeper.getInstance().getCurrentNumUser().get(loop.getLoopId());
					double delayUser = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
					TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
					double newAverageUser = (currentAverageUser*currentCountUser + delayUser)/(currentCountUser+1);
					TimeKeeper.getInstance().getCurrentAverageUser().put(loop.getLoopId(), newAverageUser);
					TimeKeeper.getInstance().getCurrentNumUser().put(loop.getLoopId(), currentCountUser+1);
					//System.out.println("tupactualid "+ tuple.getActualTupleId()+" cloudletid "+ tuple.getCloudletId()+ " Mobid " + mobileid + " starttime "+ startTime+ " delayUser "+ delayUser);
					//System.out.println("currentaverageuser "+currentAverageUser + "currentcountuser "+currentCountUser + "newAverageUser " +newAverageUser);					
				} //ragaa 
////////////////////////////////////////////////////////////////////////////////////////////////////////				
				TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				break;
			}
		}
	}

///////source module/////////////////////////////////////////////////////////////////////////////
	/*
	protected void updateTimingsOnReceipt(Tuple tuple) {
		Application app = getApplicationMap().get(tuple.getAppId());
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName();
		List<AppLoop> loops = app.getLoops();
		for(AppLoop loop : loops){
			if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){				
				Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				if(startTime==null)
					break;
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
					TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
				}
				double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
				int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
				double delay = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
				TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
				TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
				break;
			}
		}
	}
	*/
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected void processSensorJoining(SimEvent ev){
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
	}
	//////////////////////////////////////////////////////////////////////////////
	protected void executeTuple(SimEvent ev, String moduleName){
		
		Logger.debug(getName(), "Executing tuple on module "+moduleName);		
		Tuple tuple = (Tuple)ev.getData();
		//System.out.println(getName() + " Executing tuple on module "+moduleName+ "ev "+ev + "tuple"+ tuple); //ragaa
		AppModule module = getModuleByName(moduleName);
		
		 //ragaa add code to ensure atomic module migration
		List<Vm> migvm = new ArrayList<Vm>();
		/*if (this.placement.equals("Module Placement Edgeward")) {			
			 FogDevice PDev= (FogDevice)CloudSim.getEntity(this.getParentId());
			 PowerHost PHost=  PDev.getHost();
			 migvm= PHost.getVmsMigratingIn();			 
			 }*/	
	migvm= this.getHost().getVmsMigratingIn();
	//System.out.println(this.getName()+ module.getName()+ " module in migration " + migvm.contains(module)+ migvm.size());
	if(!migvm.contains(module)) {						// ragaa add for module migration
		this.Executing= true;		
		if(tuple.getDirection() == Tuple.UP){
			String srcModule = tuple.getSrcModuleName();
			if(!module.getDownInstanceIdsMaps().containsKey(srcModule))
				module.getDownInstanceIdsMaps().put(srcModule, new ArrayList<Integer>());
			if(!module.getDownInstanceIdsMaps().get(srcModule).contains(tuple.getSourceModuleId()))
				module.getDownInstanceIdsMaps().get(srcModule).add(tuple.getSourceModuleId());
			
			int instances = -1;
			for(String _moduleName : module.getDownInstanceIdsMaps().keySet()){
				instances = Math.max(module.getDownInstanceIdsMaps().get(_moduleName).size(), instances);
			}
			module.setNumInstances(instances);
		}
		
		TimeKeeper.getInstance().tupleStartedExecution(tuple);
		//ragaanew assign variable resources to each vm priority time shared scheduling
		if (this.placement.equals("Enhanced Latency Differentiated Module Placement"))
			MYupdateAllocatedMips(module);
		else 
			updateAllocatedMips(moduleName);
		 //end ragaanew assign variable resources to each vm priority time shared scheduling
		
		//System.out.println();
		//System.out.println("fogdevice executetuple calls processCloudletSubmit() ++++++++++++++++++++++++++++++++++++"+ ev); //ragaa
		processCloudletSubmit(ev, false);
		//System.out.println();
		
		updateAllocatedMips(moduleName);
		/*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/
		this.Executing= false;
	}
}
//////////////////////////////////////////////////////////////////////////////////////////////	
	
	protected void processModuleArrival(SimEvent ev){
		this.idOfEndDevices = this.getApplicationMap().get("ECG").getidOfEndDevices(); // fogdevice list<integer>getidOfEndDevices- setidOfEndDevices
		this.deviceById = this.getApplicationMap().get("ECG").getdeviceById();
		this.deadlineInfo=this.getApplicationMap().get("ECG").getDeadlineInfo();
		this.deadlineInfomodule= this.getApplicationMap().get("ECG").getDeadlineInfomodule();
		this.placement= this.getApplicationMap().get("ECG").getplacement();	
		this.additionalMipsInfo= this.getApplicationMap().get("ECG").getAdditionalMipsInfo();
		this.deadlineInfomodule1= this.getApplicationMap().get("ECG").getDeadlineInfomodule1();
				
		//System.out.println("processModuleArrival ===============additionalmipinfo "+ this.additionalMipsInfo);
		
		AppModule module = (AppModule)ev.getData();
		String appId = module.getAppId();
		//if(((AppModule)ev.getData()).getName()=="Caregiver")
		//System.out.println("processModuleArrival ==============="+ ev +" module "+ ((AppModule)ev.getData()).getName()+ "ev source "+ deviceById.get(ev.getSource())+ "ev Destination "+ ev.getDestination()+ " "+ deviceById.get(ev.getDestination()).getName());
		
/////////////////////ragaanew start////////////////////////////////////////////////////////////////////////////
		///////////////////////ragaanew change vm mips if smaller than tuple mips to allow more resources for the large tuple mips users//////
		if(this.placement.equals("Enhanced Latency Differentiated Module Placement")) {
			Integer usrid= module.getPrty();
			double vmmips=0.0;
			List<Map<String, Double>> usrmoduls= deadlineInfomodule1.get(usrid);
			//System.out.println("processModuleArrival usrid " + usrid + " xmoduls "+ usrmoduls+" "+deadlineInfomodule1);
			for(Map<String, Double> y : usrmoduls){
				if (y.containsKey(module.getName())) {
					vmmips=y.get(module.getName());
					module.setMips(vmmips);
				}
			}
		}
		//System.out.println("processModuleArrival "+   module.getName()+ "  "+ module.getPrty()+" initial " + module.getMips()+ " requested "+vmmips);
		//System.out.println("updateAllocatedMips befor "+  myAppModule.getName()+ "  "+ myAppModule.getPrty()+"  " + myAppModule.getMips());
		//System.out.println("processModuleArrival "+   module.getName()+ "  "+ module.getPrty()+" newmips  " + module.getMips());
	
///////////////////ragaanew end minimize  vm mips if less than tuple mips ////////////////////////////////////////////////////////////////////////////	
			
		if(!appToModulesMap.containsKey(appId)){
			appToModulesMap.put(appId, new ArrayList<String>());
			}
		appToModulesMap.get(appId).add(module.getName());
		
//////////////ragaa code to add users appToModulesMapUsersMob= new HashMap<String, List<Pair<Integer,String>>>(); //ragaa appname, mobid,modulename
		if(!this.appToModulesMapUsers.containsKey(appId)){
			this.appToModulesMapUsers.put(appId,  new ArrayList<Pair<Integer,String>>());
			}
		
		List<Pair<Integer,String>>dumappToModulesMapUsers; Pair<Integer,String>ddumappToModulesMapUsers;
		if (idOfEndDevices.contains(this.getId())) { 
			moduledeadline = deadlineInfomodule.get(this.getId());
			dumappToModulesMapUsers= new ArrayList<Pair<Integer,String>>();
			for(String x: moduledeadline.keySet()) {
				ddumappToModulesMapUsers= new Pair<Integer,String>(this.getId(),x);
				dumappToModulesMapUsers.add(ddumappToModulesMapUsers);
				}			  
			}		
		
		AppModule mod; Map<AppModule,Double>dumAppmoduledeadline; 
		if (moduledeadline!= null) {
			dumAppmoduledeadline= new HashMap<AppModule,Double>();
			
			for(String x: moduledeadline.keySet()) {
				mod = getModuleByName(x);
				dumAppmoduledeadline.put(mod, moduledeadline.get(x));
				
			}
			this.Appmoduledeadline=dumAppmoduledeadline.entrySet()
		             .stream().sorted(Map.Entry.comparingByValue())
		             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));			
		}
		
		if((deviceById.get(ev.getDestination()).getName().startsWith("d-"))){
			this.moduledeadline.put("Filtering", 10.00);
			this.moduledeadline.put("Monitoring",5.00);			
			this.moduledeadline=moduledeadline.entrySet()
		             .stream().sorted(Map.Entry.comparingByValue())
		             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		}
		if((deviceById.get(ev.getDestination()).getName().startsWith("proxy-server"))){
			this.moduledeadline.put("Filtering", 10.00);
			this.moduledeadline.put("Monitoring",5.00);
			this.moduledeadline.put("Caregiver", 0.00);
			this.moduledeadline=moduledeadline.entrySet()
		             .stream().sorted(Map.Entry.comparingByValue())
		             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		}
		////////////////// // solve the problem of duplicating the vms ragaa		
		//System.out.println("processModuleArrival ========= "+this.getName()+ " vmlist size "+ this.getHost().getVmList().size());
		//if(!placement.equals("Module Placement Edgeward")) {
			if(!this.getHost().getVmList().contains((Vm)module)) {
				processVmCreate(ev, false); 
			}
		//}
		//else 
			//processVmCreate(ev, false);
		//System.out.println("processModuleArrival ========= "+this.getName()+ " after vmlist size "+ this.getHost().getVmList().size());
////////////////////////////////////////////////////////////////////////////////
		//ragaa try to send ack= true 
		//Vm vm = (Vm) ev.getData();
		//System.out.println(" vm " + vm.getId()+ vm.getMips()+ ((AppModule)vm).getName());
		//processVmCreate(ev, true);    
		
		//System.out.println(module.getName()+"isBeingInstantiated  "+ module.isBeingInstantiated()); //appmodule, vm classes
		if (module.isBeingInstantiated()) {
			module.setBeingInstantiated(false);
		}
		
		initializePeriodicTuples(module);  // ragaa gives error still 
		
		module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
				.getAllocatedMipsForVm(module));
	}
	/////////////////////////////////////////////////////////////////////
	//	Pair p= new Pair<Integer, String>(ev.getDestination(),((AppModule)ev.getData()).getName());
	//	appToModulesMapUsers.get(appId).add(p);
	//System.out.println("appToModulesMap "+appToModulesMap+ "         appToModulesMapUsers  " +appToModulesMapUsers);
	//System.out.println("fogdevice processModuleArrival " + module.getName() +" at "+ this.getName()+ "  all users "+idOfEndDevices);		
	//System.out.println(this.getName()+ "device id "+ this.getId()+" hostid "+ this.getHost().getId()+ " modulemap"+ appToModulesMap.get(appId));		
	//appToModulesMapUsers.get(appId).put(idOfEndDevice, module.getName());
	//System.out.println(appToModulesMap.get(appId));
	//System.out.println("fogdevice processModuleArrival " + module.getName() +" at "+ this.getName()+ "  appToModulesMapUsers "+ appToModulesMapUsers);
	
	//System.out.println("processModuleArrival ======================="+ ev +" "+ ((AppModule)ev.getData()).getName());
	
	//dumAppmoduledeadline.clear();
	//System.out.println("============================================");
	//		System.out.println("processModuleArrival idOfEndDevices " +idOfEndDevices+ " deviceById "+ deviceById);
	//System.out.println("DeadlineInfo "+ deadlineInfo);
	//System.out.println("DeadlineInfomodule "+ deadlineInfomodule);
	//System.out.println("============================================");
	//System.out.println("============================================");
	//System.out.println(this.getName()+ this.getId()+ " moduledeadline "+this.moduledeadline);		
	//System.out.println("Appmoduledeadline "+ this.Appmoduledeadline);
	//System.out.println(this.getName()+ " appToModulesMapUsers  "+ this.appToModulesMapUsers);
	//System.out.println("placement "+  placement);
/////////////////////////////////////////////////////////////////////////////////////////////////
	
	private void initializePeriodicTuples(AppModule module) {
		String appId = module.getAppId();
		Application app = getApplicationMap().get(appId);
		List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
		//System.out.println("initializePeriodicTuples periodicEdges  " + periodicEdges);
		for(AppEdge edge : periodicEdges){
			send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
		}
	}
/////	List<AppEdge> periodicEdges = new ArrayList<AppEdge>();
		//System.out.println("initializePeriodicTuples" +appId + module.getName()+periodicEdges +app+ this.getName());
		//periodicEdges= app.getPeriodicEdges(modname);
		//System.out.println("initializePeriodicTuples periodicEdges  " + periodicEdges);
////////////////////////////////////////////*/
	protected void processOperatorRelease(SimEvent ev){
		//System.out.println("fogdevice processOperatorRelease==========================================");
		
		this.processVmMigrate(ev, false);
		//this.processVmMigrate(ev, true);
	}
	
	
	protected void updateNorthTupleQueue(){
		if(!getNorthTupleQueue().isEmpty()){
			Tuple tuple = getNorthTupleQueue().poll();
			sendUpFreeLink(tuple);
		}else{
			setNorthLinkBusy(false);
		}
	}
	
	protected void sendUpFreeLink(Tuple tuple){
		double networkDelay = tuple.getCloudletFileSize()/getUplinkBandwidth();
		setNorthLinkBusy(true);
		send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
		send(parentId, networkDelay+getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
	}
	
	protected void sendUp(Tuple tuple){
		if(parentId > 0){
			if(!isNorthLinkBusy()){
				sendUpFreeLink(tuple);
			}else{
				northTupleQueue.add(tuple);
			}
		}
	}
	
	
	protected void updateSouthTupleQueue(){
		if(!getSouthTupleQueue().isEmpty()){
			Pair<Tuple, Integer> pair = getSouthTupleQueue().poll(); 
			sendDownFreeLink(pair.getFirst(), pair.getSecond());
		}else{
			setSouthLinkBusy(false);
		}
	}
	
	protected void sendDownFreeLink(Tuple tuple, int childId){
		double networkDelay = tuple.getCloudletFileSize()/getDownlinkBandwidth();
		//Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
		setSouthLinkBusy(true);
		double latency = getChildToLatencyMap().get(childId);
		send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
		send(childId, networkDelay+latency, FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
	}
	
	protected void sendDown(Tuple tuple, int childId){
		if(getChildrenIds().contains(childId)){
			if(!isSouthLinkBusy()){
				sendDownFreeLink(tuple, childId);
			}else{
				southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
			}
		}
	}
	
/////////////////////////////////////////////////////////////////////	
	protected void sendToSelf(Tuple tuple){
		//System.out.println("sendtoself ------------------------------------DATACENTER "+getId()+" TUPLE "+ tuple.getActualTupleId()+ " CALL process tuple arrival");
		send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
		//sendNow(getId(), FogEvents.TUPLE_ARRIVAL, tuple);
	}
	
	public PowerHost getHost(){
		return (PowerHost) getHostList().get(0);
	}
	public int getParentId() {
		return parentId;
	}
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	public List<Integer> getChildrenIds() {
		return childrenIds;
	}
	public void setChildrenIds(List<Integer> childrenIds) {
		this.childrenIds = childrenIds;
	}
	public double getUplinkBandwidth() {
		return uplinkBandwidth;
	}
	public void setUplinkBandwidth(double uplinkBandwidth) {
		this.uplinkBandwidth = uplinkBandwidth;
	}
	public double getUplinkLatency() {
		return uplinkLatency;
	}
	public void setUplinkLatency(double uplinkLatency) {
		this.uplinkLatency = uplinkLatency;
	}
	public boolean isSouthLinkBusy() {
		return isSouthLinkBusy;
	}
	public boolean isNorthLinkBusy() {
		return isNorthLinkBusy;
	}
	public void setSouthLinkBusy(boolean isSouthLinkBusy) {
		this.isSouthLinkBusy = isSouthLinkBusy;
	}
	public void setNorthLinkBusy(boolean isNorthLinkBusy) {
		this.isNorthLinkBusy = isNorthLinkBusy;
	}
	public int getControllerId() {
		return controllerId;
	}
	public void setControllerId(int controllerId) {
		this.controllerId = controllerId;
	}
	public List<String> getActiveApplications() {
		return activeApplications;
	}
	public void setActiveApplications(List<String> activeApplications) {
		this.activeApplications = activeApplications;
	}
	public Map<Integer, List<String>> getChildToOperatorsMap() {
		return childToOperatorsMap;
	}
	public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
		this.childToOperatorsMap = childToOperatorsMap;
	}

	public Map<String, Application> getApplicationMap() {
		return applicationMap;
	}

	public void setApplicationMap(Map<String, Application> applicationMap) {
		this.applicationMap = applicationMap;
	}

	public Queue<Tuple> getNorthTupleQueue() {
		return northTupleQueue;
	}

	public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
		this.northTupleQueue = northTupleQueue;
	}

	public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
		return southTupleQueue;
	}

	public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
		this.southTupleQueue = southTupleQueue;
	}

	public double getDownlinkBandwidth() {
		return downlinkBandwidth;
	}

	public void setDownlinkBandwidth(double downlinkBandwidth) {
		this.downlinkBandwidth = downlinkBandwidth;
	}

	public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
		return associatedActuatorIds;
	}

	public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
		this.associatedActuatorIds = associatedActuatorIds;
	}
	
	public double getEnergyConsumption() {
		return energyConsumption;
	}

	public void setEnergyConsumption(double energyConsumption) {
		this.energyConsumption = energyConsumption;
	}
	public Map<Integer, Double> getChildToLatencyMap() {
		return childToLatencyMap;
	}

	public void setChildToLatencyMap(Map<Integer, Double> childToLatencyMap) {
		this.childToLatencyMap = childToLatencyMap;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getRatePerMips() {
		return ratePerMips;
	}

	public void setRatePerMips(double ratePerMips) {
		this.ratePerMips = ratePerMips;
	}
	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public Map<String, Map<String, Integer>> getModuleInstanceCount() {
		return moduleInstanceCount;
	}

	public void setModuleInstanceCount(
			Map<String, Map<String, Integer>> moduleInstanceCount) {
		this.moduleInstanceCount = moduleInstanceCount;
	}
//////////////////////////////////////////////////Ragaa	
	private long mips;    	
	public long getMips() {
	return mips;
	}
	public void setMips(long mips) {
	this.mips = mips;
	}   
//////////////////////////////////////////////////Ragaa	
}