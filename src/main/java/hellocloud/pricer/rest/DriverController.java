package hellocloud.pricer.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

@RestController
@RequestMapping(value = "driver")
public class DriverController {
    @Autowired
    private DataSource dataSource;

    private String driverName;

    @PostConstruct
    public String getConnectionMetaData() {
        try {
            driverName = (String) JdbcUtils.extractDatabaseMetaData(dataSource, (DatabaseMetaData dbmd) -> {return dbmd.getDriverName();});
        } catch (MetaDataAccessException e) {
            throw new RuntimeException("No JDBC Driver Name", e);
        }
        return driverName;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> getDriverName(){
        return new ResponseEntity<>(driverName, HttpStatus.OK);
    }
}
