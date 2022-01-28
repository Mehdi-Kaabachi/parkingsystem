package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    public void insertTicketDataBase() {
        Connection con;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement
                    ("insert into ticket (vehicle_reg_number, parking_number, in_time) values (?,?,?)");
            ps.setString(1, "ABCDEF");
            ps.setInt(2, 1);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.execute();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() {
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Connection con = dataBaseTestConfig.getConnection();
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery
                ("select * from ticket");
        while (rs.next()) {
            assertEquals( (1), rs.getInt("id"));
            assertEquals( ("ABCDEF"), rs.getString("vehicle_reg_number"));
        }
        ResultSet rs2 = statement.executeQuery
                ("select * from parking where parking_number = 1");
        while (rs2.next()) {
            assertEquals( (0), rs2.getInt("available"));
        }
    }

    @Test
    public void testParkingLotExit() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Timestamp inTime = new Timestamp(System.currentTimeMillis());
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));

        Connection con = dataBaseTestConfig.getConnection();
        PreparedStatement ps = con.prepareStatement
                ("update ticket set in_time = ?");
        ps.setTimestamp(1, inTime);
        ps.execute();
        parkingService.processExitingVehicle();

        Timestamp outTime = new Timestamp((System.currentTimeMillis() / 1000) * 1000);

        ResultSet rs = ps.executeQuery
                ("select * from ticket");
        while (rs.next()) {
            assertEquals(1.5, rs.getDouble("price"));
            assertEquals( (outTime), rs.getTimestamp("out_time"));
        }
    }

    @Test
    public void testCheckUserEntry() {
        insertTicketDataBase();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        boolean resultCheck = parkingService.checkUserEntry("ABCDEF");

        assertTrue(resultCheck);
    }

    @Test
    public void testCheckUserExit() {
        insertTicketDataBase();
        insertTicketDataBase();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        boolean resultCheck = parkingService.checkUserExit("ABCDEF");

        assertTrue(resultCheck);
    }

    @Test
    public void testParkingWithDiscount() throws Exception {
        insertTicketDataBase();
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();

        Timestamp inTime = new Timestamp(System.currentTimeMillis());
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Connection con = dataBaseTestConfig.getConnection();
        PreparedStatement ps = con.prepareStatement
                ("update ticket set in_time = ?");
        ps.setTimestamp(1, inTime);
        ps.execute();
        parkingService.processExitingVehicle();

        ResultSet rs = ps.executeQuery
                ("select * from ticket where id = 3");
        while(rs.next())
            assertEquals(1.425, rs.getDouble("price"));
    }
}
