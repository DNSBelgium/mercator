create or replace function mercator_to_hex(input varchar)
    returns varchar
    language plpgsql
as
$$
declare
    remainder int;
    quotient numeric;
    result varchar;
BEGIN
    quotient := cast(input as numeric);
    result = '';
    LOOP
        remainder := quotient % 16;
        quotient := div(quotient, 16);
        result := cast(to_hex(remainder) as varchar) || result;
        EXIT WHEN quotient = 0;
    end loop;
    if (length(result) % 2 = 1) then result := '0' || result;
    end if;
    result := rtrim(REGEXP_REPLACE(result, '(.{2})', '\1:' , 'g'), ':');
    RETURN result;
END;
$$;

update tls_crawler.certificate set serial_number_hex = mercator_to_hex(serial_number)
where serial_number_hex is null;