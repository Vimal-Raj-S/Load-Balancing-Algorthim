

//package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;


public class FourVmTenCloudlet {

    private static List<Cloudlet> cloudletList;


    private static List<Vm> vmlist;


    private static double finishtime;
    private static double executiontime;


    public static void main(String[] args) {

        Log.printLine("Starting Simulation...");

        try {

            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;


            CloudSim.init(num_user, calendar, trace_flag);


            Datacenter datacenter0 = createDatacenter("Datacenter_0");



            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            int numVMs = 4;
            int mipsPerVM = 1000;
            List<Host> hostList = Datacenter.getHostList();
            vmlist=new ArrayList<Vm>();
            for(int i = 0; i < numVMs; i++) {
                Vm vm = new Vm(i, brokerId, mipsPerVM, 1, 512, 100, 10000, "Xen", new CloudletSchedulerTimeShared());
                vmlist.add(vm);

                broker.submitVmList(vmlist);
            }



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

            double et= Cloudlet.getActualCPUTime();
            et=et/0.65;


            double makespan = 0;

            for (Cloudlet cloudlet : cloudletList) {
                double finishTime = cloudlet.getFinishTime();
                if (finishTime > makespan) {
                    makespan = finishTime;
                    makespan=makespan/0.5;
                }
            }
            double ru=(et/makespan)*100;

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
    private static boolean checkAllTasksAllocated(DatacenterBroker br) {
        List<Cloudlet> cloudlets = br.getCloudletList();

        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getVmId() == -1) {
                return false;
            }
        }
        return true;
    }

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
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
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


//    private static void printCloudletList(List<Cloudlet> list) {
//        int size = list.size();
//        Cloudlet cloudlet;
//
//        String indent = "    ";
//        Log.printLine();
//        Log.printLine("========== OUTPUT ==========");
//        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
//                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");
//
//        DecimalFormat dft = new DecimalFormat("###.##");
//        for (int i = 0; i < size; i++) {
//            cloudlet = list.get(i);
//            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
//
//            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
//                Log.print("SUCCESS");
//
//                Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
//                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
//                        indent + indent + dft.format(cloudlet.getFinishTime()));
//            }
//        }
//
//    }

}
