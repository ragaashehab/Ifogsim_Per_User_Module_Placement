package org.fog.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.entities.Tuple;

public class TimeKeeper {

	private static TimeKeeper instance;
	
	private long simulationStartTime;
	private int count; 
	private Map<Integer, Double> emitTimes;
	private Map<Integer, Double> endTimes;
	private Map<Integer, Double> myendTimes;  // ragaa
	private Map<Integer, List<Integer>> loopIdToTupleIds;
	private Map<Integer, Double> tupleIdToCpuStartTime;
	private Map<Integer, Double> tupleIdToCpuEndTime; // ragaa
	private Map<String, Double> tupleTypeToAverageCpuTime;
	
	private Map<String, Integer> tupleTypeToExecutedTupleCount;
	
	private Map<Integer, Double> loopIdToCurrentAverage;
	private Map<Integer, Integer> loopIdToCurrentNum;
	
	private Map<Integer, Double> CurrentAverageUser;   //ragaa
	private Map<Integer, Integer> CurrentNumUser;      //ragaa
	private Map<Integer, Double> CurrentAverageUserAnalytics;   //ragaanew
	private Map<Integer, Integer> CurrentNumUserAnalytics;      //ragaa
	
	public Map<Integer, List<Map<String, Double>>> UsertupleTypeToAverageCpuTime = new HashMap<Integer, List<Map<String,Double>>>();  // mobile id has modulename with module priority
	public Map<Integer, List<Map<String, Integer>>> UsertupleTypeToExecutedTupleCount = new HashMap<Integer, List<Map<String,Integer>>>();  // mobile id has modulename with module priority

	//public Map<Integer, List<Pair<String,Double>>> UsertupleTypeToAverageCpuTime; //ragaanew mobileid, list <pair<tupletype, executiontime>>
	//public Map<Integer, List<Pair<String,Integer>>> UsertupleTypeToExecutedTupleCount; //ragaanew mobileid, list <pair<tupletype, count>>
	
	public static TimeKeeper getInstance(){
		if(instance == null)
			instance = new TimeKeeper();
		return instance;
	}
	
	public int getUniqueId(){
		return count++;
	}
////////////////////////////////////////////////////////////////////////////////////////////////	
	public void tupleStartedExecution(Tuple tuple){
		tupleIdToCpuStartTime.put(tuple.getCloudletId(), CloudSim.clock());
		tupleIdToCpuEndTime.put(tuple.getCloudletId(), 0.0);
		//System.out.println("actualtupid "+ tuple.getActualTupleId()+ " cloudletid " + tuple.getCloudletId() + " Srcmod "+ tuple.getSrcModuleName() +" Destmod "+ tuple.getDestModuleName()+ " actuatorid "+ tuple.getActuatorId()+ " cloudletlength "+tuple.getCloudletLength());
	}
////////////////////////////////////////////////////////////////////////////////////////////////	
	public void tupleEndedExecution(Tuple tuple){
		if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
			return;		
		double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
		tupleIdToCpuEndTime.put(tuple.getCloudletId(), executionTime);

		if(!tupleTypeToAverageCpuTime.containsKey(tuple.getTupleType())){
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), executionTime);			
			tupleTypeToExecutedTupleCount.put(tuple.getTupleType(), 1);
		} else{
			double currentAverage = tupleTypeToAverageCpuTime.get(tuple.getTupleType());
			int currentCount = tupleTypeToExecutedTupleCount.get(tuple.getTupleType());
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), (currentAverage*currentCount+executionTime)/(currentCount+1));			
		}		
	}
//////////////ragaanew add average execution time to each user's tuple///////////////
	///public Map<Integer, Map<String, Double>> UsertupleTypeToAverageCpuTime = new HashMap<Integer, Map<String,Double>>();  // mobile id has modulename with module priority
	//public Map<Integer, Map<String, Integer>> UsertupleTypeToExecutedTupleCount = new HashMap<Integer, Map<String,Integer>>();  // mobile id has modulename with module priority

		@SuppressWarnings("unlikely-arg-type")
		public void tupleEndedExecutionUser(Tuple tuple){ 
			Map<String,Double> tupleTypeToAverageCpuTimeUser;
			Map<String,Integer>tupleTypeToExecutedTupleCountUser;
			List<Map<String,Double>> ListtupleTypeToAverageCpuTimeUser= new ArrayList<>();
			List<Map<String,Integer>> ListtupleTypeToExecutedTupleCountUser= new ArrayList<>();
			
			if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
				return;		
			double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
			
			int mobid= (int)tuple.getTPrty(); 
			if(!UsertupleTypeToAverageCpuTime.containsKey(mobid)){ //contains mobile id
				tupleTypeToAverageCpuTimeUser= new HashMap<String,Double>();
				tupleTypeToExecutedTupleCountUser= new HashMap<String,Integer>();
				tupleTypeToAverageCpuTimeUser.put(tuple.getTupleType(),executionTime);
				tupleTypeToExecutedTupleCountUser.put(tuple.getTupleType(), 1);
				ListtupleTypeToAverageCpuTimeUser.add(tupleTypeToAverageCpuTimeUser);
				ListtupleTypeToExecutedTupleCountUser.add(tupleTypeToExecutedTupleCountUser);
				//System.out.println(" tupleTypeToAverageCpuTimeUser "+tupleTypeToAverageCpuTimeUser);
				//System.out.println(" tupleTypeToExecutedTupleCountUser "+ tupleTypeToExecutedTupleCountUser);
				UsertupleTypeToAverageCpuTime.put(mobid, ListtupleTypeToAverageCpuTimeUser);
				UsertupleTypeToExecutedTupleCount.put(mobid, ListtupleTypeToExecutedTupleCountUser);
				//System.out.println("new mobile UsertupleTypeToAverageCpuTime.get(mobid)"+UsertupleTypeToAverageCpuTime.get(mobid));
				//System.out.println("new mobile UsertupleTypeToExecutedTupleCount.get(mobid) "+UsertupleTypeToExecutedTupleCount.get(mobid));			
			}
			else{ //mobileid exist
				List<Map<String,Double>> x = UsertupleTypeToAverageCpuTime.get(mobid);
				List<Map<String,Integer>> y= UsertupleTypeToExecutedTupleCount.get(mobid);
				Map <String,Double> a1= new HashMap<String,Double>();
				Map <String,Integer> b1= new HashMap<String,Integer>();
				//System.out.println("mobile has x "+x);
				for(Map<String,Double> a : x) {
					//System.out.println(tuple.getTupleType()+ " a.keySet()"+ a.keySet()+ " a  " +a);
					if(a.containsKey(tuple.getTupleType())) { //mobileid and tupletype exist
						//System.out.println( "a" + a + "a1" + a1);
						a1=a;
						//System.out.println( "a" + a + "a1" + a1);
						}
				}
				for(Map<String,Integer> b : y) {
					//System.out.println(tuple.getTupleType()+  b.keySet()+  b.keySet()+ " b" +b);
					if(b.containsKey(tuple.getTupleType())) //mobileid and tupletype exist
						b1= b;
				}
				//System.out.println("a1 " +a1 + " b1"+ b1);
				if(!a1.isEmpty() && !b1.isEmpty()) {
					//System.out.println("not empty a1 " +a1 + " b1"+ b1);
						double currentAverage = a1.get(tuple.getTupleType());
						int currentCount = b1.get(tuple.getTupleType());
						tupleTypeToAverageCpuTimeUser= new HashMap<String,Double>();
						tupleTypeToExecutedTupleCountUser= new HashMap<String,Integer>();
						tupleTypeToAverageCpuTimeUser.put(tuple.getTupleType(), (currentAverage*currentCount+executionTime)/(currentCount+1));
						tupleTypeToExecutedTupleCountUser.put(tuple.getTupleType(), currentCount+1);
						//System.out.println(" tupleTypeToAverageCpuTimeUser "+tupleTypeToAverageCpuTimeUser);
						//System.out.println(" tupleTypeToExecutedTupleCountUser "+ tupleTypeToExecutedTupleCountUser);
						
						//System.out.println("x befor"+ x);
						//System.out.println("y befor"+ y);
						
						List<Map<String,Double>> dumx = new ArrayList<>();
						List<Map<String,Integer>> dumy= new ArrayList<>();;
						//System.out.println("dumx" + dumx +" dumy"+ dumy);

						for(Map<String,Double> r : x) {
							//System.out.println("r"+  r +"x " +x);
							if(!r.containsKey(tuple.getTupleType())) //mobileid and tupletype exist
								dumx.add(r);
						}
						//System.out.println("x after "+ dumx);
						
						for(Map<String,Integer> u : y) {
							if(!u.containsKey(tuple.getTupleType())) //mobileid and tupletype exist
								dumy.add(u);
						}
						//System.out.println("y after "+ dumy);
					
						dumx.add(tupleTypeToAverageCpuTimeUser);
						dumy.add(tupleTypeToExecutedTupleCountUser);
						//System.out.println(" x add new "+dumx);
						//System.out.println(" y add new "+ dumy);
					
						UsertupleTypeToAverageCpuTime.put(mobid, dumx);
						UsertupleTypeToExecutedTupleCount.put(mobid, dumy);
						//System.out.println("UsertupleTypeToAverageCpuTime"+UsertupleTypeToAverageCpuTime);
						//System.out.println("UsertupleTypeToExecutedTupleCount.get(mobid) "+UsertupleTypeToExecutedTupleCount.get(mobid));			
					}
				else {	 //mobile id exist but tupletype is not exist
						//System.out.println("empty a1 " +a1 + " b1"+ b1);
						tupleTypeToAverageCpuTimeUser= new HashMap<String,Double>();
						tupleTypeToExecutedTupleCountUser= new HashMap<String,Integer>();
						tupleTypeToAverageCpuTimeUser.put(tuple.getTupleType(),executionTime);
						tupleTypeToExecutedTupleCountUser.put(tuple.getTupleType(), 1);
						//System.out.println(" tupleTypeToAverageCpuTimeUser "+tupleTypeToAverageCpuTimeUser);
						//System.out.println(" tupleTypeToExecutedTupleCountUser "+ tupleTypeToExecutedTupleCountUser);
						x.add(tupleTypeToAverageCpuTimeUser);
						y.add(tupleTypeToExecutedTupleCountUser);
						
						UsertupleTypeToAverageCpuTime.put(mobid, x);
						UsertupleTypeToExecutedTupleCount.put(mobid, y);
						//System.out.println("UsertupleTypeToAverageCpuTime.get(mobid)"+UsertupleTypeToAverageCpuTime);
						//System.out.println("UsertupleTypeToExecutedTupleCount.get(mobid) "+UsertupleTypeToExecutedTupleCount);
					}
					}
			//System.out.println("UsertupleTypeToAverageCpuTime.get(mobid)"+UsertupleTypeToAverageCpuTime);
			//System.out.println("UsertupleTypeToExecutedTupleCount.get(mobid) "+UsertupleTypeToExecutedTupleCount);
			//System.out.println("==========================================================================================");
			}		
							
		public Map<Integer,List< Map<String,Double>>> getUsertupleTypeToAverageCpuTime(){ //ragaanew mobileid, list <pair<tupletype, executiontime>>
			return UsertupleTypeToAverageCpuTime;
		}
		public void setUsertupleTypeToAverageCpuTime(Map<Integer, List<Map<String,Double>>> UsertupleTypeToAverageCpuTime){ //ragaanew mobileid, list <pair<tupletype, executiontime>>
			this.UsertupleTypeToAverageCpuTime=UsertupleTypeToAverageCpuTime;
		}
		
		public Map<Integer, List<Map<String,Integer>>> getUsertupleTypeToExecutedTupleCount(){ //ragaanew mobileid, list <pair<tupletype, count>>
			return UsertupleTypeToExecutedTupleCount;
		}
		public void setUsertupleTypeToExecutedTupleCount(Map<Integer, List<Map<String,Integer>>> UsertupleTypeToExecutedTupleCount){ //ragaanew mobileid, list <pair<tupletype, count>>
			this.UsertupleTypeToExecutedTupleCount=UsertupleTypeToExecutedTupleCount;
		}

////////////////////////////end ragaanew//////////////////////////////////////////////////////////		
////////////////////////////////////////////////////////////////////////////////////////////////ragaa
	
	public void tupleEndedExecutioncloudlets(Tuple tuple){
		if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
			return;
		double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
		//System.out.println("tuple"+ tuple.getActualTupleId()+ tuple.getTupleType()+"\t"+ "cloudletid"+ tuple.getCloudletId() +"\t"+ "starttime "+tupleIdToCpuStartTime.get(tuple.getCloudletId())+"\t"+ " execultion time" + executionTime );
		if(!myendTimes.containsKey(tuple.getCloudletId())){
			myendTimes.put(tuple.getCloudletId(), executionTime);
			//myendTimesoExecutedTupleCount.put(tuple.getTupleType(), 1);
		} else{
			double currentcloudletexecutiontimes = myendTimes.get(tuple.getCloudletId());
			//int currentCount = tupleTypeToExecutedTupleCount.get(tuple.getTupleType());
			myendTimes.put(tuple.getCloudletId(), (currentcloudletexecutiontimes + executionTime));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////end ragaa/////////*/
	public Map<Integer, List<Integer>> loopIdToTupleIds(){
		return getInstance().getLoopIdToTupleIds();
	}
	
	private TimeKeeper(){
		count = 1;
		setEmitTimes(new HashMap<Integer, Double>());
		setEndTimes(new HashMap<Integer, Double>());
		setMyEndTimes(new HashMap<Integer, Double>());    // ragaa
		setLoopIdToTupleIds(new HashMap<Integer, List<Integer>>());
		setTupleTypeToAverageCpuTime(new HashMap<String, Double>());
		setTupleTypeToExecutedTupleCount(new HashMap<String, Integer>());
		setTupleIdToCpuStartTime(new HashMap<Integer, Double>());
		setTupleIdToCpuEndTime(new HashMap<Integer, Double>());   // ragaa
		setLoopIdToCurrentAverage(new HashMap<Integer, Double>());
		setLoopIdToCurrentNum(new HashMap<Integer, Integer>());
		setLoopIdToCurrentAverageUserAnalytics(new HashMap<Integer, Map<Integer,Double>>());   //ragaanew
		setLoopIdToCurrentNumUserAnalytics(new HashMap<Integer, Map<Integer, Integer>>());  //ragaanew
		setCurrentAverageUser(new HashMap<Integer, Double>());  // ragaa
		setCurrentNumUser(new HashMap<Integer, Integer>());    // ragaa
		setUsertupleTypeToAverageCpuTime(new HashMap<Integer, List<Map<String,Double>>>());
		setUsertupleTypeToExecutedTupleCount(new HashMap<Integer, List<Map<String,Integer>>>());
}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Map<Integer, Double> getEmitTimes() {
		return emitTimes;
	}

	public void setEmitTimes(Map<Integer, Double> emitTimes) {
		this.emitTimes = emitTimes;
	}

	public Map<Integer, Double> getEndTimes() {
		return endTimes;
	}

	public void setEndTimes(Map<Integer, Double> endTimes) {
		this.endTimes = endTimes;
	}
	
////////////////////////////////////////////ragaa/////////////////////////////////////////
	public Map<Integer, Double> getMyEndTimes() {
		return myendTimes;
	}
	
	public void setMyEndTimes(Map<Integer, Double> myendTimes) {
		this.myendTimes = myendTimes;
	}
//////////////////////////////////////////////////////////////////////////////////////
	public Map<Integer, List<Integer>> getLoopIdToTupleIds() {
		return loopIdToTupleIds;
	}

	public void setLoopIdToTupleIds(Map<Integer, List<Integer>> loopIdToTupleIds) {
		this.loopIdToTupleIds = loopIdToTupleIds;
	}

	public Map<String, Double> getTupleTypeToAverageCpuTime() {
		return tupleTypeToAverageCpuTime;
	}

	public void setTupleTypeToAverageCpuTime(
			Map<String, Double> tupleTypeToAverageCpuTime) {
		this.tupleTypeToAverageCpuTime = tupleTypeToAverageCpuTime;
	}

	public Map<String, Integer> getTupleTypeToExecutedTupleCount() {
		return tupleTypeToExecutedTupleCount;
	}

	public void setTupleTypeToExecutedTupleCount(
			Map<String, Integer> tupleTypeToExecutedTupleCount) {
		this.tupleTypeToExecutedTupleCount = tupleTypeToExecutedTupleCount;
	}

	public Map<Integer, Double> getTupleIdToCpuStartTime() {
		return tupleIdToCpuStartTime;
	}

	public void setTupleIdToCpuStartTime(Map<Integer, Double> tupleIdToCpuStartTime) {
		this.tupleIdToCpuStartTime = tupleIdToCpuStartTime;
	}
////////////////////////////////////////////////////////// ragaa//////////////////////////////////////////////
	public Map<Integer, Double> getTupleIdToCpuEndTime() {
		return tupleIdToCpuEndTime;
	}

	public void setTupleIdToCpuEndTime(Map<Integer, Double> tupleIdToCpuEndTime) {
		this.tupleIdToCpuEndTime = tupleIdToCpuEndTime;
	}
/////////////////////////////////////////////////////////

	public long getSimulationStartTime() {
		return simulationStartTime;
	}

	public void setSimulationStartTime(long simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
	}

	public Map<Integer, Double> getLoopIdToCurrentAverage() {
		return loopIdToCurrentAverage;
	}

	public void setLoopIdToCurrentAverage(Map<Integer, Double> loopIdToCurrentAverage) {
		this.loopIdToCurrentAverage = loopIdToCurrentAverage;
	}

	public Map<Integer, Integer> getLoopIdToCurrentNum() {
		return loopIdToCurrentNum;
	}

	public void setLoopIdToCurrentNum(Map<Integer, Integer> loopIdToCurrentNum) {
		this.loopIdToCurrentNum = loopIdToCurrentNum;
	}
	
//////////////////////////////ragaanew useranalytics//////////////////////////////////////
////////////////////////////////ragaanew/////////user analytics///////////////////////////////////
Map<Integer, Map<Integer,Double>> loopIdToCurrentAverageUserAnalytics; 
Map<Integer, Map<Integer,Integer>> loopIdToCurrentNumUserAnalytics;
public Map<Integer, Map<Integer,Double>> getLoopIdToCurrentAverageUserAnalytics() {		
return loopIdToCurrentAverageUserAnalytics;
}
public void setLoopIdToCurrentAverageUserAnalytics(Map<Integer, Map<Integer,Double>> loopIdToCurrentAverageUserAnalytics) {
this.loopIdToCurrentAverageUserAnalytics = loopIdToCurrentAverageUserAnalytics;
}
public Map<Integer, Map<Integer,Integer>> getLoopIdToCurrentNumUserAnalytics() {
return loopIdToCurrentNumUserAnalytics;
}
public void setLoopIdToCurrentNumUserAnalytics(Map<Integer, Map<Integer,Integer>> loopIdToCurrentNumUserAnalytics) {
this.loopIdToCurrentNumUserAnalytics = loopIdToCurrentNumUserAnalytics;
}	
public Map<Integer, Double> getCurrentAverageUserAnalytics() {   //ragaanew mobileid, average analytics delay for the user
return CurrentAverageUserAnalytics;
}

public void setCurrentAverageUserAnalytics(Map<Integer, Double> CurrentAverageUserAnalytics) {  //ragaanew
this.CurrentAverageUserAnalytics = CurrentAverageUserAnalytics;
}

public Map<Integer, Integer> getCurrentNumUserAnalytics() {    //ragaanew mobileid, num of received cloudlets  
return CurrentNumUser;
}

public void setCurrentNumUserAnalytics(Map<Integer, Integer> CurrentNumUserAnalytics) {  //ragaanew
this.CurrentNumUserAnalytics = CurrentNumUserAnalytics;
}
/// ragaanew user analytics end//////////////////////////////////////////
////////////////////////////////////////////////////////ragaa//////////////////////////////////////////////
	public Map<Integer, Double> getCurrentAverageUser() {   // mobileid, average delay for the user
		return CurrentAverageUser;
	}

	public void setCurrentAverageUser(Map<Integer, Double> CurrentAverageUser) {  
		this.CurrentAverageUser = CurrentAverageUser;
	}

	public Map<Integer, Integer> getCurrentNumUser() {    //ragaa mobileid, num of received cloudlets  
		return CurrentNumUser;
	}

	public void setCurrentNumUser(Map<Integer, Integer> CurrentNumUser) {
		this.CurrentNumUser = CurrentNumUser;
	}

	public  Map<Integer, Double> gettupleEndedExecutioncloudlets() {  //ragaanew
		// TODO Auto-generated method stub
		return myendTimes;
	}
//////////////////////////////////////////////////////////////////////////////////////////////////////
}

////////////////////////////////////////////////////////////////////////////////
/*public void tupleEndedExecutionUser(Tuple tuple){ //pair structure ,,,,
		Pair<String,Double> tupleTypeToAverageCpuTimeUser= new Pair<String,Double>(null,null);
		Pair<String,Integer>tupleTypeToExecutedTupleCountUser= new Pair<String,Integer>(null,null);						
		List<Pair<String,Double>> ListtupleTypeToAverageCpuTimeUser = new ArrayList<Pair<String,Double>>();
		List<Pair<String,Integer>>ListtupleTypeToExecutedTupleCountUser = new ArrayList<Pair<String,Integer>>();
		List<Pair<String,Double>> dumListtupleTypeToAverageCpuTimeUser = new ArrayList<Pair<String,Double>>();
		List<Pair<String,Integer>>dumListtupleTypeToExecutedTupleCountUser = new ArrayList<Pair<String,Integer>>();
		double currentAverage=0.0;
		int currentCount=0;

		if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
			return;		
		double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
		tupleIdToCpuEndTime.put(tuple.getCloudletId(), executionTime);
		int mobid= (int)tuple.getTPrty(); 
		if(!UsertupleTypeToAverageCpuTime.containsKey(mobid)){ //contains mobile id
			//UsertupleTypeToAverageCpuTime.put(mobid, new ArrayList<Pair<String,Double>>());
			//UsertupleTypeToExecutedTupleCount.put(mobid, new ArrayList<Pair<String,Integer>>())
			//System.out.println("UsertupleTypeToAverageCpuTime "+UsertupleTypeToAverageCpuTime);
			
			tupleTypeToAverageCpuTimeUser= new Pair<String,Double>(tuple.getTupleType(),executionTime);
			tupleTypeToExecutedTupleCountUser=new Pair<String,Integer>(tuple.getTupleType(), 1);
			//System.out.println(" tupleTypeToAverageCpuTimeUser "+tupleTypeToAverageCpuTimeUser);
			//System.out.println(" tupleTypeToExecutedTupleCountUser "+ tupleTypeToExecutedTupleCountUser);
			
			//ListtupleTypeToAverageCpuTimeUser= UsertupleTypeToAverageCpuTime.get(mobid);
			//ListtupleTypeToExecutedTupleCountUser= UsertupleTypeToExecutedTupleCount.get(mobid);
			
			ListtupleTypeToAverageCpuTimeUser.add(tupleTypeToAverageCpuTimeUser);
			ListtupleTypeToExecutedTupleCountUser.add(tupleTypeToExecutedTupleCountUser);
			//System.out.println("ListtupleTypeToAverageCpuTimeUser "+ListtupleTypeToAverageCpuTimeUser);
			//System.out.println("ListtupleTypeToExecutedTupleCountUser "+ListtupleTypeToExecutedTupleCountUser);
			
			UsertupleTypeToAverageCpuTime.put(mobid, ListtupleTypeToAverageCpuTimeUser);
			UsertupleTypeToExecutedTupleCount.put(mobid, ListtupleTypeToExecutedTupleCountUser);

			//System.out.println("UsertupleTypeToAverageCpuTime.get(mobid)"+this.UsertupleTypeToAverageCpuTime);
			//this.UsertupleTypeToAverageCpuTime.get(mobid).addAll(ListtupleTypeToAverageCpuTimeUser);
			//this.UsertupleTypeToExecutedTupleCount.get(mobid).addAll(ListtupleTypeToExecutedTupleCountUser);
			//System.out.println("UsertupleTypeToExecutedTupleCount "+this.UsertupleTypeToExecutedTupleCount);			
		}
		else{				
			ListtupleTypeToAverageCpuTimeUser= UsertupleTypeToAverageCpuTime.get(mobid);
			ListtupleTypeToExecutedTupleCountUser= UsertupleTypeToExecutedTupleCount.get(mobid);
		//	if(ListtupleTypeToAverageCpuTimeUser.contains((tuple.getTupleType()))
				
				for(Pair<String,Double> p1: ListtupleTypeToAverageCpuTimeUser){
					//System.out.println(tuple.getTupleType()+ "p1" +p1);
					if(p1.getKey().equals(tuple.getTupleType())) {	//mobileid and tupletype exist 				
						tupleTypeToAverageCpuTimeUser= new Pair<String, Double>(p1.getKey(), p1.getValue());
						currentAverage =tupleTypeToAverageCpuTimeUser.getValue();
						//System.out.println(tuple.getTupleType()+ "tupleTypeToAverageCpuTimeUser"+tupleTypeToAverageCpuTimeUser);
						break;
					}
					else {	 //mobile id exist but tupletype is not exist
						tupleTypeToAverageCpuTimeUser= new Pair<String,Double>(tuple.getTupleType(),executionTime);
					}
					}
	
				System.out.println("currentAverage"+currentAverage+"tupleTypeToAverageCpuTimeUser"+tupleTypeToAverageCpuTimeUser);
				
				for(Pair<String,Integer> p2: ListtupleTypeToExecutedTupleCountUser){
					if(p2.getKey().equals(tuple.getTupleType())) {					
						tupleTypeToExecutedTupleCountUser= new Pair<String, Integer>(p2.getKey(), p2.getValue());
						currentCount =tupleTypeToExecutedTupleCountUser.getValue();
						//System.out.println("tupleTypeToExecutedTupleCountUser"+tupleTypeToExecutedTupleCountUser);
						break;
					}
					else {
						tupleTypeToExecutedTupleCountUser=new Pair<String,Integer>(tuple.getTupleType(), 1);
				}}
				System.out.println("currentCount"+ currentCount+"tupleTypeToExecutedTupleCountUser"+tupleTypeToExecutedTupleCountUser);			

				//if(!tupleTypeToExecutedTupleCountUser.getKey().equals(tuple.getTupleType()))
				//System.out.println("tupleTypeToExecutedTupleCountUser"+tupleTypeToExecutedTupleCountUser);
				
				double newaverage= (currentAverage*currentCount+executionTime)/(currentCount+1);
				tupleTypeToAverageCpuTimeUser= new Pair<String, Double>(tuple.getTupleType(), newaverage);
				tupleTypeToExecutedTupleCountUser= new Pair<String, Integer>(tuple.getTupleType(), currentCount+1);
				System.out.println("new tupleTypeToAverageCpuTimeUser"+tupleTypeToAverageCpuTimeUser);
				System.out.println("new tupleTypeToExecutedTupleCountUser"+tupleTypeToExecutedTupleCountUser);
				//ListtupleTypeToAverageCpuTimeUser.add(tupleTypeToAverageCpuTimeUser);
				//ListtupleTypeToExecutedTupleCountUser.add(tupleTypeToExecutedTupleCountUser);

				for(Pair<String,Double> p1: ListtupleTypeToAverageCpuTimeUser){
					if(p1.getKey().equals(tuple.getTupleType())){	//mobileid and tupletype exist 				
						ListtupleTypeToAverageCpuTimeUser.remove(p1);
						//System.out.println(tuple.getTupleType()+ "tupleTypeToAverageCpuTimeUser"+tupleTypeToAverageCpuTimeUser);
					}
				}
				ListtupleTypeToAverageCpuTimeUser.add(tupleTypeToAverageCpuTimeUser);
				
				for(Pair<String,Integer> p5: ListtupleTypeToExecutedTupleCountUser){
					if(p5.getKey().equals(tuple.getTupleType())) {	//mobileid and tupletype exist 				
						ListtupleTypeToExecutedTupleCountUser.remove(p5);
						//System.out.println(tuple.getTupleType()+ "tupleTypeToAverageCpuTimeUser"+tupleTypeToAverageCpuTimeUser);
					}
				}
				ListtupleTypeToExecutedTupleCountUser.add(tupleTypeToExecutedTupleCountUser);
									
			    System.out.println(" ListtupleTypeToAverageCpuTimeUser" +ListtupleTypeToAverageCpuTimeUser);
				System.out.println(" ListtupleTypeToExecutedTupleCountUser" +ListtupleTypeToExecutedTupleCountUser);
								
				UsertupleTypeToAverageCpuTime.get(mobid).addAll(ListtupleTypeToAverageCpuTimeUser);
				UsertupleTypeToExecutedTupleCount.get(mobid).addAll(ListtupleTypeToExecutedTupleCountUser);
			}
	}
	*/