#floorplan

# set power grid
derive_pg_connection -power_net VDD -power_pin VDD -create_port top
derive_pg_connection -ground_net VSS -ground_pin VSS -create_port top

set_preferred_routing_direction   -layers {M1 M3 M5 M7} -direction horizontal
set_preferred_routing_direction   -layers {M2 M4 M6 } -direction vertical

# set I/O pins
set_pin_physical_constraints -layers {M5} -side 1 -offset 50 -pin_spacing 5 [get_ports]
set_fp_pin_constraints \
  -block_level \
  -use_physical_constraints on \
  -corner_keepout_percent_side 15 \
  -keep_buses_together on

create_floorplan \
        -core_utilization 0.7 \
        -flip_first_row \
        -start_first_row \
  	-left_io2core 5 \
  	-bottom_io2core 5 \
  	-right_io2core 5 \
  	-top_io2core 5 \

# there's something funky about the synopsys 32nm educational library
# which results in create_floorplan leaving gaps between the standard cell rows it creates
# removing and regenerating the standard cell rows seems to get around the propblem
cut_row -all
add_row \
  -within [get_attr [get_core_area] bbox] \
  -direction horizontal \
  -flip_first_row \
  -tile_name unit

# create initial placement
#create_fp_placement -effort high -timing_driven
create_fp_placement -effort high -timing_driven -optimize_pins

# create power distribution network
synthesize_fp_rings \
  -nets {VDD VSS} \
  -core \
  -layers {M5 M4} \
  -width {1.25 1.25} \
  -space {0.5 0.5} \
  -offset {1 1}

set_power_plan_strategy core \
  -nets {VDD VSS} \
  -core \
  -template saed_32nm.tpl:m45_mesh(0.5,1.0) \
  -extension {stop: outermost_ring}

#set macros [collection_to_list -no_braces -name_only [get_cells -hierarchical -filter "mask_layout_type==macro"]]
#set_power_plan_strategy macros \
  -nets {VDD VSS} \
  -macros $macros \

compile_power_plan

# add filler cells so all cell sites are populated
# synopsys recommends you do this before routing standard cell power rails
insert_stdcell_filler   \
        -cell_without_metal "SHFILL128_RVT SHFILL64_RVT SHFILL3_RVT SHFILL2_RVT SHFILL1_RVT" \
        -connect_to_power {VDD} \
        -connect_to_ground {VSS}

# preroute standard cell rails
preroute_standard_cells \
  -within [get_attribute [get_core_area] bbox] \
  -connect horizontal     \
  -port_filter_mode off   \
  -cell_master_filter_mode off    \
  -cell_instance_filter_mode off  \
  -voltage_area_filter_mode off \
  -do_not_route_over_macros \
  -no_routing_outside_working_area \

# get rid of filler cells
remove_stdcell_filler -stdcell

# verify connectivity of power/ground nets
verify_pg_nets

