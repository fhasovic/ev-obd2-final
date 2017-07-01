package com.reversecoder.obd2.utils;

/**
 * @author Md. Rashsadul Alam
 */
public class CommandList {

    public static final String EQUIVALENT_RATION = "01 44";
    public static final String TIMING_ADVANCE = "01 0E";
    public static final String VIN = "09 02";
    public static final String RUNTIME = "01 1F";
    public static final String MASS_AIR_FLOW = "01 10";
    public static final String FIND_FUEL_TYPE = "01 51";
    public static final String CONSUMPTION_RATE = "01 5E";
    public static final String FUEL_LEVEL = "01 2F";
    public static final String FUEL_TRIM_LONG_TERM_BANK_1 = "01 00x07";
    public static final String FUEL_TRIM_LONG_TERM_BANK_2 = "01 00x08";
    public static final String FUEL_TRIM_SHORT_TERM_BANK_1 = "01 00x06";
    public static final String FUEL_TRIM_SHORT_TERM_BANK_2 = "01 00x09";
    public static final String AIR_FUEL_RATION = "01 44";
    public static final String OIL_TEMPERATURE_ENGINE = "01 5C";
    public static final String BAROMETRIC_PRESSURE = "01 33";
    public static final String FUEL_PRESSURE = "01 0A";
    public static final String FUEL_RAIL_PRESSURE = "01 23";
    public static final String AMBIENT_AIR_TEMPERATURE = "01 46";
    public static final String WIDE_BAND_AIR_FUEL_RATION = "01 34";
    
    /*
    * Commands for EV
    * */
    public static final String RESET = "AT Z";
    public static final String ECHO_OFF = "AT E0";
    public static final String LINE_FEED_OFF = "AT L0";
    public static final String TIMEOUT = "AT ST 3e";
    public static final String PROTOCOL_AUTO = "AT SP 0";
    public static final String SPEED = "01 0D";
    public static final String DISTANCE_MIL_ON = "01 21";
    public static final String DTC_NUMBER = "01 01";
    public static final String ENGINE_COOLANT_TEMP = "01 05";
    public static final String AIR_INTAKE_TEMPERATURE = "01 0F";
    public static final String RPM = "01 0C";
    public static final String INTAKE_MANIFOLD_PRESSURE = "01 0B";
    public static final String MODULE_VOLTAGE = "01 42";
    public static final String TROUBLE_CODES = "03";
    public static final String LOAD = "01 04";
    public static final String THROTTLE_POSITION = "01 11";
}
