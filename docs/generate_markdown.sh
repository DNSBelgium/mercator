# script to convert CSV file to a markdown table (to avoid having to manually convert from property name to environment variable format)

duckdb << EOF
.mode markdown
.once mercator_settings.MD

select property_name, replace(upper(property_name), '.', '_') as environment_variable, description, Data_type, default_value
from read_csv('props.csv', header=true, column_names=['property_name', 'description', 'Data_type', 'default_value'], strict_mode=false, null_padding=true)
order by property_name;

EOF


