package com.reversecoder.obd.reader.config;

import com.reversecoder.obd2.commands.ObdCommand;
import com.reversecoder.obd2.commands.SpeedCommand;
import com.reversecoder.obd2.commands.control.DistanceMILOnCommand;
import com.reversecoder.obd2.commands.control.DtcNumberCommand;
import com.reversecoder.obd2.commands.control.EquivalentRatioCommand;
import com.reversecoder.obd2.commands.control.ModuleVoltageCommand;
import com.reversecoder.obd2.commands.control.TimingAdvanceCommand;
import com.reversecoder.obd2.commands.control.TroubleCodesCommand;
import com.reversecoder.obd2.commands.control.VinCommand;
import com.reversecoder.obd2.commands.engine.LoadCommand;
import com.reversecoder.obd2.commands.engine.MassAirFlowCommand;
import com.reversecoder.obd2.commands.engine.OilTempCommand;
import com.reversecoder.obd2.commands.engine.RPMCommand;
import com.reversecoder.obd2.commands.engine.RuntimeCommand;
import com.reversecoder.obd2.commands.engine.ThrottlePositionCommand;
import com.reversecoder.obd2.commands.fuel.AirFuelRatioCommand;
import com.reversecoder.obd2.commands.fuel.ConsumptionRateCommand;
import com.reversecoder.obd2.commands.fuel.FindFuelTypeCommand;
import com.reversecoder.obd2.commands.fuel.FuelLevelCommand;
import com.reversecoder.obd2.commands.fuel.FuelTrimCommand;
import com.reversecoder.obd2.commands.fuel.WidebandAirFuelRatioCommand;
import com.reversecoder.obd2.commands.pressure.BarometricPressureCommand;
import com.reversecoder.obd2.commands.pressure.FuelPressureCommand;
import com.reversecoder.obd2.commands.pressure.FuelRailPressureCommand;
import com.reversecoder.obd2.commands.pressure.IntakeManifoldPressureCommand;
import com.reversecoder.obd2.commands.temperature.AirIntakeTemperatureCommand;
import com.reversecoder.obd2.commands.temperature.AmbientAirTemperatureCommand;
import com.reversecoder.obd2.commands.temperature.EngineCoolantTemperatureCommand;
import com.reversecoder.obd2.enums.FuelTrim;

import java.util.ArrayList;

/**
 * TODO put description
 */
public final class ObdConfig {

    public static ArrayList<ObdCommand> getCommands() {
        ArrayList<ObdCommand> cmds = new ArrayList<>();

        // Control
        cmds.add(new ModuleVoltageCommand());//
//        cmds.add(new EquivalentRatioCommand());
        cmds.add(new DistanceMILOnCommand());//
        cmds.add(new DtcNumberCommand());//
//        cmds.add(new TimingAdvanceCommand());
        cmds.add(new TroubleCodesCommand());//
//        cmds.add(new VinCommand());

        // Engine
        cmds.add(new LoadCommand());//
        cmds.add(new RPMCommand());//
//        cmds.add(new RuntimeCommand());
//        cmds.add(new MassAirFlowCommand());
        cmds.add(new ThrottlePositionCommand());//

        // Fuel
//        cmds.add(new FindFuelTypeCommand());
//        cmds.add(new ConsumptionRateCommand());
//        // cmds.add(new AverageFuelEconomyObdCommand());
//        //cmds.add(new FuelEconomyCommand());
//        cmds.add(new FuelLevelCommand());
//        // cmds.add(new FuelEconomyMAPObdCommand());
//        // cmds.add(new FuelEconomyCommandedMAPObdCommand());
//        cmds.add(new FuelTrimCommand(FuelTrim.LONG_TERM_BANK_1));
//        cmds.add(new FuelTrimCommand(FuelTrim.LONG_TERM_BANK_2));
//        cmds.add(new FuelTrimCommand(FuelTrim.SHORT_TERM_BANK_1));
//        cmds.add(new FuelTrimCommand(FuelTrim.SHORT_TERM_BANK_2));
//        cmds.add(new AirFuelRatioCommand());
//        cmds.add(new WidebandAirFuelRatioCommand());//
//        cmds.add(new OilTempCommand());

        // Pressure
//        cmds.add(new BarometricPressureCommand());
//        cmds.add(new FuelPressureCommand());
//        cmds.add(new FuelRailPressureCommand());
//        cmds.add(new IntakeManifoldPressureCommand());//

        // Temperature
        cmds.add(new AirIntakeTemperatureCommand());//
//        cmds.add(new AmbientAirTemperatureCommand());
        cmds.add(new EngineCoolantTemperatureCommand());//

        // Misc
        cmds.add(new SpeedCommand());//


        return cmds;
    }

    public static ArrayList<ObdCommand> getElectricVehicleCommands() {
        ArrayList<ObdCommand> cmds = new ArrayList<>();

        cmds.add(new RPMCommand());//

        // Misc
        cmds.add(new SpeedCommand());//


        return cmds;
    }


}
