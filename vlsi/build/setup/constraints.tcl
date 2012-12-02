# create clock
create_clock clk -name clk -period ${CLOCK_PERIOD}
set_clock_uncertainty ${CLOCK_UNCERTAINTY} [get_clocks clk]

# set drive strength for inputs
set_driving_cell -lib_cell INVX1_RVT [all_inputs]
# set load capacitance of outputs
set_load -pin_load 0.004 [all_outputs]

# Set timing contraints for the input and output I/O ports
set all_inputs_but_clk [remove_from_collection [all_inputs] [get_ports clk]]
# disregard timing of image_width, image_height, reset inputs
# (assuming they will be stable for a long time)
set dim_inputs [get_ports io_image_*]
set_false_path -from $dim_inputs
set_false_path -from [get_ports reset]

set all_inputs_but_dim [remove_from_collection $all_inputs_but_clk $dim_inputs]
set_input_delay ${INPUT_DELAY} -clock [get_clocks clk] $all_inputs_but_dim
set_output_delay ${OUTPUT_DELAY} -clock [get_clocks clk] [all_outputs]

# preserve (parts of) hierarchy
set_ungroup windowBuf5x5 false
set_ungroup windowBuf5x5/* true
set_ungroup windowBuf2x2x2 false
set_ungroup windowBuf2x2x2/* true
set_ungroup windowBuf3x3 false
set_ungroup windowBuf3x3/* true

set_ungroup gaussian false
set_ungroup gaussian/* true
set_ungroup partialDeriv false
set_ungroup partialDeriv/* true

set_ungroup uvIteration false
set_ungroup uvIteration/uvAvg true

# label libraries by threshold voltage
set dbfile [lindex $TARGET_LIBRARY_FILES 0] 
set len [string length $dbfile]
set HVT_lib [string range $dbfile 0 [expr "$len - 4"]]
echo HVT_lib = $HVT_lib

set dbfile [lindex $TARGET_LIBRARY_FILES 1] 
set len [string length $dbfile]
set RVT_lib [string range $dbfile 0 [expr "$len - 4"]]
echo RVT_lib = $RVT_lib

set dbfile [lindex $TARGET_LIBRARY_FILES 2] 
set len [string length $dbfile]
set LVT_lib [string range $dbfile 0 [expr "$len - 4"]]
echo LVT_lib = $LVT_lib

set_attribute [get_libs $LVT_lib] default_threshold_voltage_group LVt -type string
set_attribute [get_libs $RVT_lib] default_threshold_voltage_group RVt -type string
set_attribute [get_libs $HVT_lib] default_threshold_voltage_group HVt -type string

# alternative to set_leakage_optimization true
# another power optimization option is to specify max percentage of cells to 
# use from specified Vt libraries
#set_multi_vth_constraint -lvth_groups {LVT} -lvth_percentage 5.0 -type soft

