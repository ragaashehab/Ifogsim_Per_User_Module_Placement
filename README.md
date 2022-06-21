# Ifogsim_Per_User_Module_Placement
This work presents Fog Assisted Resource Management FARM platform based on Apache Hadoop 2 (YARN) for short term and long term data analytics. 
Static FARM (S-FARM) is proposed for module placement using per user and per module modes, it represents the YARN ResourceManager (Scheduler). 
S-FARM algorithms are simulated over iFogSim as follow: 
YARN Scheduler is represented by:
    controller class,
    per-user-Basic-ModulePlacement class,
    per-module-ModulePlacement class,
    per-user-Diﬀerentiated-ModulePlacement class,
    StreamOperatorScheduler class (extends VmSchedulerTimeSharedOverbookingEnergy),
    TupleScheduler class (extends CloudletSchedulerDynamicWorkload),
YARN ApplicationManager is represented by:
    CheckAnalyticsDelay-Enhanced-Diﬀerentiated method in the FogDevice class,
    MY-updateAllocatedMips method in the FogDevice class (enhanced Time Shared Scheduling).
YARN NodeManager (slaves): Represented by FogDevice class.
YARN Container: Represented by AppModule class
YARN ApplicationMaster: Represented by the main class
YARN Application: DAG of jobs Represented by the Application class

Stream CardioVascular Disease (S-CVD) application is modeled and simulated to test the proposed YARN schedulers.
S-CVD lively analyzes the patient’s ECG streams to conduct the patient’s state using a linear classiﬁer.
