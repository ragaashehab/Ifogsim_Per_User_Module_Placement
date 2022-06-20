package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ControllerEdgeward;
import org.fog.placement.ControllerHybrid;
import org.fog.placement.LatencyHybridModulePlacementNear;
import org.fog.placement.LatencyModulePlacementNear;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for  ECG
 * @author Ragaa Shehab
 *
 */ //updateTimingsOnReceipt - checkCloudletCompletion --tupleEndedExecutionUser
public class ECG {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static Map<Integer,FogDevice> deviceById = new HashMap<Integer,FogDevice>();	
	static List<Integer> idOfEndDevices = new ArrayList<Integer>();
	static Map<Integer, Double> deadlineInfo = new	HashMap<Integer, Double>();  // mobile id has priority
	static Map<Integer, Double> additionalMipsInfo = new HashMap<Integer, Double>(); // mobile id need tuple mips
	static Map<Integer, Map<String, Double>> deadlineInfomodule = new HashMap<Integer, Map<String,Double>>();  // mobile id has modulename with module priority
	static Map<Integer,List<Map<String, Double>>> deadlineInfomodule1 = new HashMap<Integer, List<Map<String,Double>>>();  // mobile id has modulename with module priority

	static boolean CLOUD = false;
	static int numOfDepts = 1;
	static int numOfMobilesPerDept =3;
	static double ECG_TRANSMISSION_TIME = 50.0 ; // 50.0;   //100, 50, 25 ms   100 //10s= 10000
	static public int mymips= 2000;     //500,1000,2000,2500,3000,4000
	static String placement = "Latency Module Placement"; 
	//static String placement = "Latency Differentiated Module Placement"; 
	//static String placement = "Hybrid Module Placement";
	//static String placement = "Enhanced Latency Differentiated Module Placement"; 
	
	static double MobileCPUload= 0.1;
	
	public static void main(String[] args) {
		Log.print("Starting ECG transmission");	Log.print("             ECG transmission time ms: "); Log.print(ECG_TRANSMISSION_TIME);
		Log.print("     Depts/Mobiles =  "); Log.print (numOfDepts);Log.print("/"); Log.printLine (numOfMobilesPerDept);
		System.out.println("analytics VM mips  2000, Fog device mips: mobile 1000, dept  2800, proxy 16800" ); 

		
		try {
			Log.disable(); 	
			//Config.MAX_SIMULATION_TIME= 100000;
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events
			CloudSim.init(num_user, calendar, trace_flag);						
			String appId = "ECG"; 
			FogBroker broker = new FogBroker("broker");			
			createFogDevices(broker.getId(), appId);
			
			Map<String, Double> moduledeadline= new HashMap<String, Double>();
			Map<String, Double> moduledeadline1;
			List<Map<String, Double>> moduledeadline1List;

			for(Integer id : idOfEndDevices){
				Double y= getvalue(5.00, 40.00);
				Double t1= 10.00; Double t2= 5.00; //Double t3= 5.00;
				Double x= y;
				deadlineInfo.put(id, x);
				
				if (placement.equals("Latency Differentiated Module Placement")||placement.equals("Enhanced Latency Differentiated Module Placement")) {
					if((5.00 < y)&& (y < 10.00)) x=5.00; // 0.25,   0.5,   1,   1.5 vm mips
						if((10.00 < y)&& (y < 15.00)) x=10.00;
						if((15.00 < y)&& (y < 20.00)) x=15.00;
						if((20.00 < y)&& (y < 30.00)) x=20.00;
						if((30.00 <  y)&& (y < 40.00)) x=30.00;
						
					additionalMipsInfo.put(id, x*100); //1000
					moduledeadline1=  new HashMap<String,Double>();
					moduledeadline1List= new ArrayList<Map<String, Double>>();
					moduledeadline1.put("Client", 2000.0);
					moduledeadline1.put("Filtering", x*100);
					moduledeadline1.put("Monitoring", x*100);
					moduledeadline1.put("Caregiver", x*100);
					moduledeadline1.put("Cloud_Analytics", 3000.0);
					moduledeadline1List.add(moduledeadline1);
					deadlineInfomodule1.put(id, moduledeadline1List);
					/*
					if((5.00 < y)&& (y < 10.00)) x=5.00; // 50:400
					if((10.00 < y)&& (y < 15.00)) x=10.00;
					if((15.00 < y)&& (y < 20.00)) x=15.00;
					if((20.00 < y)&& (y < 30.00)) x=20.00;
					if((30.00 <  y)&& (y < 35.00)) x=30.00;
					if((35.00 <  y)&& (y < 40.00)) x=40.00;
					
				additionalMipsInfo.put(id, x*10); //1000
				moduledeadline1=  new HashMap<String,Double>();
				moduledeadline1List= new ArrayList<Map<String, Double>>();
				moduledeadline1.put("Client", 2000.0);
				moduledeadline1.put("Filtering", x*10);
				moduledeadline1.put("Monitoring", x*10);
				moduledeadline1.put("Caregiver", x*10);
				moduledeadline1.put("Cloud_Analytics", 3000.0);
				moduledeadline1List.add(moduledeadline1);
				deadlineInfomodule1.put(id, moduledeadline1List);
				*/
				}
				else {
					additionalMipsInfo.put(id, (double)mymips);
					moduledeadline1=  new HashMap<String,Double>();
					moduledeadline1List= new ArrayList<Map<String, Double>>();
					moduledeadline1.put("Client", 2000.0);
					moduledeadline1.put("Filtering", (double)mymips);
					moduledeadline1.put("Monitoring", (double)mymips);
					moduledeadline1.put("Caregiver", (double)mymips);
					moduledeadline1.put("Cloud_Analytics", 3000.0);
					moduledeadline1List.add(moduledeadline1);
					deadlineInfomodule1.put(id, moduledeadline1List);
				}
				moduledeadline.put("Filtering", (x+t1));
				deadlineInfomodule.put(id,moduledeadline);
				moduledeadline.put("Monitoring", (x+ t2));
				deadlineInfomodule.put(id,moduledeadline);
			}	
			
			deadlineInfo=deadlineInfo.entrySet()
		             .stream().sorted(Map.Entry.comparingByValue())
		             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));			
				
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			application.setdeviceById(deviceById);
			application.setidOfEndDevices(idOfEndDevices);
			application.setDeadlineInfo(deadlineInfo);
			application.setDeadlineInfomodule(deadlineInfomodule);
			application.setDeadlineInfomodule1(deadlineInfomodule1);
		    application.setAdditionalMipsInfo(additionalMipsInfo);
		    application.setplacement(placement);
		    application.setMobileCPUload(MobileCPUload);
		    
		    System.out.println(String.format("%-25s%s" ,"User Priority ASC ", application.getDeadlineInfo()));
			System.out.println(String.format("%-25s%s" ,"Module Tuble MIPS ",application.getAdditionalMipsInfo()));
		   /// System.out.println(String.format("%-25s%s" ,"Module Priority ",application.getDeadlineInfomodule()));
		    System.out.println(String.format("%-25s%s" ,"Module Priority1 ",application.getDeadlineInfomodule1()));

		    System.out.println("mobile CPU load "+ application.getMobileCPUload());
		    		
			//Controller controller = null;
			//ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping			
			
			if(CLOUD){
				ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping			
				moduleMapping.addModuleToDevice("Cloud_Analytics", "cloud");
				moduleMapping.addModuleToDevice("Caregiver", "cloud");
				moduleMapping.addModuleToDevice("Monitoring", "cloud");
				moduleMapping.addModuleToDevice("Filtering", "cloud");
				for(FogDevice device : fogDevices){
					if(device.getName().startsWith("m"))
						moduleMapping.addModuleToDevice("Client", device.getName());
					}
				System.out.println("cloud only module placement, tuple mips= "+ mymips);
				Controller controller = new ControllerEdgeward("master-controller", fogDevices, sensors, actuators);
				controller.submitApplication(application, 0, new ModulePlacementMapping(fogDevices, application,moduleMapping));
				}
			else {
				ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping			
				moduleMapping.addModuleToDevice("Cloud_Analytics", "cloud");
				moduleMapping.addModuleToDevice("Caregiver", "proxy-server");
				for(FogDevice device : fogDevices){
					if(device.getName().startsWith("m"))
						moduleMapping.addModuleToDevice("Client", device.getName());
				//	if(device.getName().startsWith("d")) {
					//	moduleMapping.addModuleToDevice("Monitoring",  device.getName());
					//	moduleMapping.addModuleToDevice("Filtering",  device.getName());
					   //}
				 }
				//System.out.println("PER USER FIXED ANALYTIC MODULE PLACEMENT FOR DEPT RESOURCE MANAGEMENT");
				//Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
				//controller.submitApplication(application, 0, new LatencyModulePlacementNear(fogDevices, sensors, actuators, application, moduleMapping));
			//}
				switch (placement) { 
				case "Latency Module Placement":{			
					System.out.println("Per User Basic module placement");//", Tuple mips= "+ mymips
					Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
					controller.submitApplication(application, 0, new LatencyModulePlacementNear(fogDevices, sensors, actuators, application, moduleMapping));
					break;}	
				case "Latency Differentiated Module Placement":{
					System.out.println("Per User Differentiated module placement, variable Module/Tuple mips ");
					Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
					controller.submitApplication(application, 0, new LatencyModulePlacementNear(fogDevices, sensors, actuators, application, moduleMapping));
					break;} //ModulePlacementDifferentiated
				case "Hybrid Module Placement":{
					System.out.println("hybrid module placement");
					ControllerHybrid controller = new ControllerHybrid("master-controller", fogDevices, sensors, actuators);
					controller.submitApplication(application, 0, new LatencyHybridModulePlacementNear(fogDevices, sensors, actuators, application, moduleMapping));
					break;}
				}
			}
					
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			CloudSim.startSimulation();
			CloudSim.stopSimulation();
			Log.printLine("ECG finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}	
/****************************************************************************************
	 * Creates the fog devices in the physical topology of the simulation.
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {  // 44800
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
		cloud.setParentId(-1);		 
		FogDevice proxy = createFogDevice("proxy-server", 16800 , 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // 2800
		proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
		proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms		
		fogDevices.add(cloud);
		deviceById.put(cloud.getId(), cloud);
		fogDevices.add(proxy);
		deviceById.put(proxy.getId(), proxy);		
		for(int i=0;i<numOfDepts;i++){
			addGw(i+"", userId, appId, proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
		}	
	}

	private static FogDevice addGw(String id, int userId, String appId, int parentId){
		FogDevice dept = createFogDevice("d-"+id, 2800, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333); //2800
		fogDevices.add(dept);
		deviceById.put(dept.getId(), dept);
		dept.setParentId(parentId);
		dept.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms
		for(int i=0;i<numOfMobilesPerDept;i++){
			String mobileId = id+"-"+i;
			FogDevice mobile = addMobile(mobileId, userId, appId, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
			mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 4 ms
			fogDevices.add(mobile);
			deviceById.put(mobile.getId(), mobile);
		}
		return dept;
	}
	
	private static FogDevice addMobile(String id, int userId, String appId, int parentId){
		FogDevice mobile = createFogDevice("m-"+id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44); //1000   
		mobile.setParentId(parentId);
		idOfEndDevices.add(mobile.getId());
		Sensor ecgsensor = new Sensor("s-"+id, "ECG", userId, appId, new DeterministicDistribution(ECG_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
		sensors.add(ecgsensor);
		Actuator display = new Actuator("a-"+id, userId, appId, "DISPLAY");
		actuators.add(display);
		ecgsensor.setGatewayDeviceId(mobile.getId());
		ecgsensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
		display.setGatewayDeviceId(mobile.getId());
		display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
		return mobile;
	}
	
/*****************************************************************************************
	 * Creates a vanilla fog device
	 * @param nodeName name of the device to be used in simulation
	 * @param mips MIPS
	 * @param ram RAM
	 * @param upBw uplink bandwidth
	 * @param downBw downlink bandwidth
	 * @param level hierarchy level of the device
	 * @param ratePerMips cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
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
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this	// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10 , upBw, downBw, 0, ratePerMips); //10 : schedulingInterval  0 uplink  latency		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}

/******************************************************************************************
	 * Function to create the EEG Tractor Beam game application in the DDF model. 
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)
		
		// Adding modules (vertices) to the application model (directed graph)
		application.addAppModule("Client", 10, 2000, 1000,100,5); //250Name,int ram, int mips, long size, long bw, int modprty) size is the image size (MB)
		application.addAppModule("Filtering",10, 2000, 500, 100,4); //150
		application.addAppModule("Monitoring",10, 2000, 500,100,3); //150 
		application.addAppModule("Caregiver", 10, 2000,1000,100,2); // 150
		application.addAppModule("Cloud_Analytics",10, 3000,1000,250,1); //500("Cloud_Analytics",10, 500,1000,500,1); //
		
		// Connecting the application modules (vertices) in the application model (directed graph) with edges
		//source, destination, double tupleCpuLength, double tupleNwLength, String tupleType, int direction, int edgeType){
		application.addAppEdge("ECG", "Client", 2000, 500, "ECG", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("Client", "Filtering", 2000, 500, "SENSOR_DATA", Tuple.UP, AppEdge.MODULE); //
		application.addAppEdge("Filtering", "Monitoring", mymips, 500, "FILTERED_DATA", Tuple.UP, AppEdge.MODULE); //
		application.addAppEdge("Monitoring", "Caregiver", mymips, 500, "ECG_REPORT", Tuple.UP, AppEdge.MODULE);  //
		application.addAppEdge("Caregiver", "Cloud_Analytics", mymips, 500, "DATA_OFFLOAD", Tuple.UP, AppEdge.MODULE); //		
		application.addAppEdge("Caregiver", "Client", 2000, 500, "State_Action", Tuple.DOWN, AppEdge.MODULE); //  
		application.addAppEdge("Client", "DISPLAY",2000, 500, "P_State_Action", Tuple.DOWN, AppEdge.ACTUATOR);  //
		application.addAppEdge("Cloud_Analytics", "Caregiver", 2000, 1000, "Advanced_Analytics", Tuple.DOWN, AppEdge.MODULE);  
		
 	    //Defining the input-output relationships (represented by selectivity) of the application modules. 
		application.addTupleMapping("Client", "ECG", "SENSOR_DATA", new FractionalSelectivity(1.0)); // 
		application.addTupleMapping("Client", "State_Action", "P_State_Action", new FractionalSelectivity(1.0)); //  
		application.addTupleMapping("Filtering", "SENSOR_DATA", "FILTERED_DATA", new FractionalSelectivity(1.0)); // 
		application.addTupleMapping("Monitoring", "FILTERED_DATA", "ECG_REPORT", new FractionalSelectivity(1.0)); //
		application.addTupleMapping("Caregiver", "ECG_REPORT", "DATA_OFFLOAD", new FractionalSelectivity(1.0)); //
		application.addTupleMapping("Caregiver", "ECG_REPORT", "State_Action", new FractionalSelectivity(1.0)); //
		application.addTupleMapping("Cloud_Analytics", "DATA_OFFLOAD", "Advanced_Analytics", new FractionalSelectivity(0.01)); //
		//application.addTupleMapping("Caregiver", "Advanced_Analytics", "State_Action", new FractionalSelectivity(1.0)); //
		
		 // Defining application loops to monitor the latency of.
		//final AppLoop ONLY_CLOUD = new AppLoop(new ArrayList<String>(){{add("ECG");add("Client");add("Filtering");add("Monitoring");add("Caregiver");add("Client");add("DISPLAY");}}); //add("Cloud_Analytics");
        //List<AppLoop> loops = new ArrayList<AppLoop>(){{ add(ONLY_CLOUD);}};
		//final AppLoop ECG_REPORTING = new AppLoop(new ArrayList<String>(){{add("ECG");add("Client");add("Filtering");add("Monitoring");add("Caregiver");add("Client");add("DISPLAY");}});
        //List<AppLoop> loops = new ArrayList<AppLoop>(){{ add(ECG_REPORTING);}};
		final AppLoop ECG_ANALYTICS = new AppLoop(new ArrayList<String>(){{add("ECG");add("Client");add("Filtering");add("Monitoring");add("Caregiver");}});
        List<AppLoop> loops = new ArrayList<AppLoop>(){{add( ECG_ANALYTICS);}}; 
 		application.setLoops(loops);
		return application;
		}
///random number generators//////////////////////////////////////////////////////////
	private static double getvalue(double d, double e)  { 
	    Random r = new Random(); 
	    double randomValue = d + (e - d) *r.nextDouble(); 
	   // randomValue = r.nextDouble();
	    return randomValue; 
	  } 
	private static Integer getvalue(int min, int max) { 
	    Random r = new Random(); 
	    Integer randomValue = min + r.nextInt()%(max - min); 
	   // randomValue = r.nextInt(max);
	    return randomValue; 
	  }
}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//scheduled cloudlet processing =30,100,200,300,500 mips for filtering monitoring caregiver  
//scheduled cloudlet processing = 1000  for client and cloudanalytics modules
// mobile id, need Modulename executed with max delay (3:5)
//static Pair<String, Double> moduledeadline = new Pair <String, Double>(null,null);  // mobile id need deadline double
//static List<Pair<String, Double>> moduledeadline = new ArrayList<Pair <String, Double>>();


//if(placement.equals("Latency Differentiated Module Placement")) {		
//System.out.println(String.format("%-30s%s" ,"Per_User Differentiated Module Placement, " ," variable mips between 50 mips, "+  mymips+ " for all modules except cloud plus 1000 mips"));
//}		
//else if(placement.equals("Module Placement Edgeward")) {
//System.out.println(String.format("%-30s%20s%s%30s%10s%s" ,"Per_Module Edgeward Placement","const mips= " , mymips));
//System.out.println("Per_Module Placement,  Tuple's mips= " + mymips);
//}
///else 
//placement=="Latency Module Placement" 
//System.out.println(String.format("%-30s%20s%s%30s%10s%s" ,"Per_User Basic Module Placement","const mips= " , mymips));
//System.out.println("Per_User Basic Module Placement,  const mips= " + mymips);		

//System.out.println("Application Module mips: Client = Filtering = Monitoring = Caregiver = 300 mips , Cloud_Analytics = 600 mips");
//System.out.println("Migration Condition: Mobile Utilization >= 0.3, Proxy/Dept Utilization >= 0.9" );  //, RESOURCE_MANAGE_INTERVAL = 100 ");
//System.out.println("Enhanced Migration Conditions , RESOURCE_MANAGE_INTERVAL = 10 ");
//System.out.println("fog device scheduling interval = 10,  Application module scheduling interval = 300  ");	

//moduledeadline.put("Client", (x+ 15.0000000000000000));
//deadlineInfomodule.put(id,moduledeadline);
//moduledeadline.put("Caregiver", x);
//deadlineInfomodule.put(id,moduledeadline);				
//System.out.println("deadlineInfomodule  "+ deadlineInfomodule);
//deadlineInfomodule.put(id,moduledeadlinelist);				
//moduledeadline.clear();

/*
for(Integer id : idOfEndDevices){
	if (placement.equals("Latency Differentiated Module Placement")) {
		Integer x= getvalue(50, 500);//mymips);
		x= Math.abs(x);
		additionalMipsInfo.put(id, (double)x);  //getvalue(0,mymips)
		}
	else
		additionalMipsInfo.put(id, (double)mymips);
}
*/
/*else if(device.getName().startsWith("d")){
moduleMapping.addModuleToDevice("Filtering", device.getName());
moduleMapping.addModuleToDevice("Monitoring", device.getName());				
} */
//Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
//controller.submitApplication(application, 0, new ModulePlacementOnlyCloud(fogDevices, sensors, actuators, application));
//controller.submitApplication(application, 0, new LatencyModulePlacement(fogDevices, sensors, actuators, application, moduleMapping));
//controller.submitApplication(application, 0, new LatencyModifiedModulePlacementNew(fogDevices, sensors, actuators, application, moduleMapping));
 //LModulePlacementEdgewards  // LatencyModulePlacementNear

//TRY FogDevice cloud = createFogDevice("cloud", 448000000, 40000000, 100000, 10000000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
//FogDevice cloud = createFogDevice("cloud", 4480000, 4000000, 100, 10000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
//FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);

//peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating


/*
application.addAppModule("Client", 10, 300, 1000,100,5); // adding module Client to the application model
application.addAppModule("Filtering",10, 300, 1000, 100,4); //
application.addAppModule("Monitoring",10, 300, 1000,100,3); // 
application.addAppModule("Caregiver", 10, 300,1000,100,2); // 
application.addAppModule("Cloud_Analytics",10, 600,1000,250,1); //("Cloud_Analytics",10, 500,1000,500,1); //
*/
/*application.addAppEdge("ECG", "Client", 3000, 1000, "ECG", Tuple.UP, AppEdge.SENSOR);
application.addAppEdge("Client", "Filtering", 3000, 500, "SENSOR_DATA", Tuple.UP, AppEdge.MODULE); //
application.addAppEdge("Filtering", "Monitoring", (30+ mymips), 500, "FILTERED_DATA", Tuple.UP, AppEdge.MODULE); // 
application.addAppEdge("Monitoring", "Cloud_Analytics", 1000, 500, "DATA_OFFLOAD", Tuple.UP, AppEdge.MODULE); // 
application.addAppEdge("Monitoring", "Caregiver", (60+ mymips) , 500, "ECG_REPORT", Tuple.UP, AppEdge.MODULE);  // 
application.addAppEdge("Cloud_Analytics", "Caregiver", 3500, 1000, "Advanced_Analytics", Tuple.DOWN, AppEdge.MODULE);  // 
application.addAppEdge("Caregiver", "Client", 1000 , 500, "State_Action", Tuple.DOWN, AppEdge.MODULE); // 
application.addAppEdge("Client", "DISPLAY",1000, 500, "P_State_Action", Tuple.DOWN, AppEdge.ACTUATOR);  //
 */
//application.addAppEdge("Caregiver", "Cloud_Analytics", 3000, 1000, "DECISION", Tuple.UP, AppEdge.MODULE); //
// application.addTupleMapping("Cloud_Analytics", "DECISION", "Advanced_Analytics", new FractionalSelectivity(0.001)); //
//final AppLoop HBR_Analytics = new AppLoop(new ArrayList<String>(){{add("HBR"); add("Client"); add("Monitoring"); add("Cloud_Analytics"); add("Caregiver"); add("Client"); add("DISPLAY");}});


//case "Latency Modified Module Placement":{
//System.out.println("Per User Modified module placement");
//Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
//controller.submitApplication(application, 0, new ModulePlacementModified(fogDevices, sensors, actuators, application, moduleMapping));
//break;}
/*
 * case "Module Placement Edgeward":{
 * System.out.println("Per module placement"); Controller controller = new
 * ControllerEdgeward("master-controller", fogDevices, sensors, actuators);
 * controller.submitApplication(application, 0, new
 * LModulePlacementEdgewards(fogDevices, sensors, actuators, application,
 * moduleMapping)); break;} case "Module Placement Edgeward Differentiated":{
 * System.out.println("Per module Differentiated placement"); Controller
 * controller = new ControllerEdgeward("master-controller", fogDevices, sensors,
 * actuators); controller.submitApplication(application, 0, new
 * LModulePlacementEdgewards(fogDevices, sensors, actuators, application,
 * moduleMapping)); break;}
 */
//static String placement = "Module Placement Edgeward";
	//static String placement = "Module Placement Edgeward Differentiated";
	//static String placement = "Latency Modified Module Placement";