package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean isDiscount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inHour = ticket.getInTime().getTime();
        long outHour = ticket.getOutTime().getTime();

        long duration = (outHour - inHour) / (60 * 1000); //get time in minute

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                if (duration >= 30 && duration < 60) //if parking time is between 30 and 59 minutes give 3/4th parking fare
                    ticket.setPrice(Fare.CAR_RATE_PER_HOUR * 0.75);
                else if (duration >= 1440) //if parking time is more than 1440 minutes (24 hours) give 24 * parking fare
                    ticket.setPrice(Fare.CAR_RATE_PER_HOUR * 24);
                else if (duration < 30) //if parking time is less than 30 minutes set price to 0
                    ticket.setPrice(0);
                else
                    ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR  / 60); //divide per 60 to obtain parking fare per hour
                break;
            }
            case BIKE: {
                if (duration >= 30 && duration< 60)
                    ticket.setPrice(Fare.BIKE_RATE_PER_HOUR * 0.75);
                else if (duration >= 1440)
                    ticket.setPrice(Fare.BIKE_RATE_PER_HOUR * 24);
                else if (duration < 30)
                    ticket.setPrice(0);
                else
                    ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR / 60);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
        if (isDiscount) {
            ticket.setPrice(ticket.getPrice() - (ticket.getPrice() / 100 * 5)); //reduce price by 5 percent
        }
    }
}