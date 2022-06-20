package org.fog.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.entities.FogDevice;

public class AppModuleAllocationPolicy extends VmAllocationPolicy{

	private Host fogHost;
	
	private List<Integer> appModuleIds;
	
	
	public AppModuleAllocationPolicy(List<? extends Host> list) {
		super(list);
		if(list.size()==1)
			fogHost = list.get(0);
		appModuleIds = new ArrayList<Integer>();
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		Host host = fogHost;		
		boolean result = host.vmCreate(vm);
		if (result) { // if vm were succesfully created in the host
			getAppModuleIdsIds().add(vm.getId());
			//System.out.print("appmoduleallocationpolicy " + host.getDatacenter().getName()+ " allocate "+((AppModule)vm).getName() + " mips "+ vm.getMips()+" vm listsize "+ host.getVmList().size() + " AppModuleIdsIds " + getAppModuleIdsIds()+ " list of vms mips ");			
			} // " host " + host.getId() +
		for(Vm a: host.getVmList()) {
			//System.out.print(a.getId()+"   "+ a.getMips()+ "\t");
		}
		//System.out.println();
		
		return result;
	}

	@Override
	public boolean allocateHostForVm(Vm vm, Host host) {		
		boolean result = host.vmCreate(vm);
		if (result) { // if vm were succesfully created in the host	
				getAppModuleIdsIds().add(vm.getId());			
		//System.out.println("appmoduleallocationpolicy " + host.getDatacenter().getName()+ " allocate "+((AppModule)vm).getName()+ " mips "+ vm.getMips()+ " vmlist size "+ host.getVmList().size());
		}		// " host " + host.getId() + " AppModuleIdsIds " + getAppModuleIdsIds()
		return result;
	}
	//	if(!getAppModuleIdsIds().contains(vm.getId()))
	//System.out.println("appmoduleallocationpolicy getAppModuleIdsId befor)"+ host.getVmList()+ getAppModuleIdsIds());
	//getAppModuleIdsIds().clear();
	//for(Vm vmlst: host.getVmList())
		//getAppModuleIdsIds().add(vmlst.getId());
	//System.out.println("appmoduleallocationpolicy VMlist "+ host.getVmList()+ " AppModuleIdsId "+ getAppModuleIdsIds());
	//////////////////////////////////////////////////////////////

	@Override
	public List<Map<String, Object>> optimizeAllocation(
			List<? extends Vm> vmList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deallocateHostForVm(Vm vm) {
		if (fogHost != null) {
			fogHost.vmDestroy(vm);
		}
	}

	@Override
	public Host getHost(Vm vm) {
		return fogHost;
	}

	@Override
	public Host getHost(int vmId, int userId) {
			return fogHost;
	}

	public Host getFogHost() {
		return fogHost;
	}

	public void setFogHost(Host fogHost) {
		this.fogHost = fogHost;
	}

	public List<Integer> getAppModuleIdsIds() {
		return appModuleIds;
	}

	public void setAppModuleIds(List<Integer> appModuleIds) {
		this.appModuleIds = appModuleIds;
	}


}
