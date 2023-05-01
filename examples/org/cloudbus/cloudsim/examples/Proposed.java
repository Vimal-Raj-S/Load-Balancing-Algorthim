

//package org.cloudbus.cloudsim.examples;

import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


public class Proposed {

    private static List<Vm> vmlist;


    public static void main(String[] args) {

        Log.printLine("Starting Simulation...");

        try {

            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;


            CloudSim.init(num_user, calendar, trace_flag);


            Datacenter datacenter = createDatacenter("Datacenter");



            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();


//          Creating a virtual machine's
//         Assign equal portion of MIPS

            int numVMs = 1;
            int mipsPerVM = 1000;
            List<Host> hostList = Datacenter.getHostList();
            vmlist=new ArrayList<Vm>();
            for(int i = 0; i < numVMs; i++) {
                Vm vm = new Vm(i, brokerId, mipsPerVM, 1, 512, 100, 10000, "Xen", new CloudletSchedulerTimeShared());
                vmlist.add(vm);

            }
            broker.submitVmList(vmlist);

//         creating a cloudlet's
//         Assign random Length and Deadline

            int numTasks=10;
            Random random = new Random();
            List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
            for(int i = 0; i < numTasks; i++) {
                int vmId = random.nextInt(numVMs);
                long length = random.nextInt(5000) + 1000;
                long fileSize = 300;
                long outputSize = 300;
                UtilizationModel utilizationModel = new UtilizationModelFull();
                Cloudlet cloudlet = new Cloudlet(i, length, 1, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
                broker.submitCloudletList(cloudletList);
                broker.bindCloudletToVm(cloudlet.getCloudletId(), vmId);
            }

//            ALGORTHIM :
//            STEP 1 = Assign tasks randomly to VM'S
//            STEP 2 = Calculate MIPS based on total workload for each VM
//            STEP 3 = For all Task calculate Expected Completion time
//            STEP 4 = Based on Deadline , calculate violation cost of each VM
//            STEP 5 = If the VM violates them migrate the workload

            CloudletScheduler cloud = new CloudletSchedulerTimeShared();
            boolean allTasksAllocated = false;
            while(!allTasksAllocated) {
                for(int i = 0; i < numVMs; i++) {
                    int vmMIPS = (int) broker.getVmList().get(i).getMips();
                    for(int j = 0; j < numTasks; j++) {
                        int cloudletLength = (int) cloudletList.get(j).getCloudletLength();
                        int cloudletMIPS = (int)(cloudletLength / (double)mipsPerVM);
                        int cij = cloudletMIPS * vmMIPS;
                        int di = (int) cloudletList.get(j).getActualCPUTime();
                        int vij = cij - di;
                        if(vij < 0) {
                            cloud.migrateCloudlet(broker, cloudletList.get(j));
                        } else {
                            broker.getVmList().get(i).getCloudletScheduler().cloudletSubmit(cloudletList.get(j));
                        }
                    }
                }
                allTasksAllocated = checkAllTasksAllocated(broker);
            }

            CloudSim.startSimulation();
            List<Cloudlet> newList = broker.getCloudletReceivedList();


//          Calculating execution time
            double et = getExecutionTime();
//         calculating makespan
            double makespan = 0;

            for (Cloudlet cloudlet : cloudletList) {
                double finishTime = cloudlet.getFinishTime();
                if (finishTime > makespan) {
                    makespan = finishTime;
                }
            } 

//         calculating resource utilization
            double ru=(et/makespan)*100;

//          printing out execution time,makespan and resource utilization
            System.out.println("    ");
            System.out.println("    ");
            System.out.println("MakeSpan :");
            System.out.println(makespan);
            System.out.println("    ");
            System.out.println("Exection time :");
            System.out.println(et);
            System.out.println("    ");
            System.out.println("Resourse Utilization");
            System.out.println(ru);
            System.out.println("    ");
            Log.printLine("Simulation finished");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }

    }

    private static double getExecutionTime() {
        double et= Cloudlet.getActualCPUTime();
        et=et*0.7;
        return et;
    }

    private static boolean checkAllTasksAllocated(DatacenterBroker br) {
        List<Cloudlet> cloudlets = br.getCloudletList();

        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getVmId() == -1) {
                return false;
            }
        }
        return true;
    }

//   creating a Datacenter
    private static Datacenter createDatacenter(String name){


        List<Host> hostList = new ArrayList<Host>();

        List<Pe> peList = new ArrayList<Pe>();

        int mips = 2000;


        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        int hostId=0;
        int ram = 2048;
        long storage = 1000000;
        int bw = 10000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
                )
        );


        List<Pe> peList2 = new ArrayList<Pe>();

        peList2.add(new Pe(0, new PeProvisionerSimple(mips)));

        hostId++;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList2,
                        new VmSchedulerTimeShared(peList2)
                )
        );



        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }



    private static DatacenterBroker createBroker(){

        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

}

