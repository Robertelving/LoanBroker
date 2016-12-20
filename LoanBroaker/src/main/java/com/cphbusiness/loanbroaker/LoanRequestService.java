/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cphbusiness.loanbroaker;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Skroget
 */
@WebService(serviceName = "LoanRequestService")
public class LoanRequestService {

    /**
     * Web service operation
     */
    @WebMethod(operationName = "MakeLoanRequest")
    public String MakeLoanRequest(@WebParam(name = "ssn") String ssn, @WebParam(name = "loanAmount") double loanAmount, @WebParam(name = "loanDuration") int loanDuration, @WebParam(name = "loanCurrency") String loanCurrency) {
        String reply = "";
        try {
            reply = LoanRequest(ssn,loanAmount,loanDuration, loanCurrency);
        } catch (TimeoutException ex) {
            Logger.getLogger(LoanRequestService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return reply;
    }

    ConnectionFactory factory;
    Connection connection;
    Channel channel;

    public String LoanRequest(String ssn, double loanAmount, int loanDuration, String currency) throws TimeoutException {

        String EXCHANGE_NAME = "routing_exchange";
        
        if (ssn.matches("[0-9]{6}-([0-9]{4})") && loanAmount > 0 && loanDuration > 0) {

            String loanRequest = "<RequestObject><ssn>"+ssn+"</ssn><amount>"+loanAmount+"</amount><duration>"+loanDuration+"</duration><currency>"+currency+"</currency></RequestObject>";

            try {
                factory = new ConnectionFactory();
                factory.setHost("localhost");
                factory.setPort(5672);
                factory.setUsername("user");
                factory.setPassword("password");

                connection = factory.newConnection();
             
                channel = connection.createChannel();

                                
             
                
                channel.queueDeclare(ssn,true,false,false,null);
                channel.queueBind(ssn, EXCHANGE_NAME, ssn);
                
                
                channel.basicPublish(EXCHANGE_NAME, "credit", null, loanRequest.getBytes());
                
                QueueingConsumer qc = new QueueingConsumer(channel);
                
                try{
                    channel.basicConsume(ssn, true, qc);
                    QueueingConsumer.Delivery delivery = qc.nextDelivery();
                    String str = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    return str;
                }catch(Exception ex){
                    System.out.println("ERROR");
                }finally{                
                    channel.close();
                    connection.close();
                }
               
                

                return "User requested a loan: " + loanRequest;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "Something went wrong in 'LoanRequestImplementation' Class";
    }

}
