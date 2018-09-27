package org.opentcs.constants;

public interface LSDefaultConstants {

    /*接收车辆发来的命令类型   start*/
    final static String EVENT_NAME_COMMANDFINISHED="commandFinished";
    final static String R_COMMAND_TYPE_UPDATE_POSITION ="updatePosition";
    final static String R_COMMAND_TYPE_UPDATE_PRECISEPOSITION ="updatePrecisePosition";
    final static String R_COMMAND_TYPE_UPDATE_STATE ="updateState";
    final static String R_COMMAND_TYPE_CMD_EXCUTED ="cmdExcuted";
    /*接收车辆发来的命令类型   end*/


    /*向车辆发送的命令类型   start*/
    final static String S_COMMAND_TYPE_ABORT="abortAssignedOrder";
    final static String S_COMMAND_TYPE_MOVE="move";
    final static String S_COMMAND_TYPE_TERMINATE="terminate";
    final static String S_COMMAND_TYPE_CONNECT_STATE="connectState";
    /*向车辆发送的命令类型   end*/

    final static String BROADCAST_TYPE_CHECKRMISERVER="CHECKRMISERVER";
    final static String BROADCAST_RESULT_RMISERVER="RMISERVER";
}
