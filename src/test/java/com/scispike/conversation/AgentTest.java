package com.scispike.conversation;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.amchealth.callback.Callback;
import com.amchealth.callback.Event;
import com.amchealth.callback.EventEmitter;
import com.amchealth.conversation.Agent;
import com.amchealth.mqtt_client_api.Socket;
import com.amchealth.test.Util;

/*
 * Prior to running this tests you should:
 * sudo npm install -g conversation 
 * conversation create test
 * cd test
 * npm run gen-src
 * npm start
 */
public class AgentTest {
  
  @Test
  public void testConnect(){
    final CountDownLatch signal = new CountDownLatch(1);
    Socket socket = Util.getSocket();
    EventEmitter<String> connectEmitter = socket.getConnectEmitter();
    String agentId = UUID.randomUUID().toString();
    final Agent agent = new Agent("test.obj", socket, agentId);
    agent.on("error", new Event<JSONObject>() {
      @Override
      public void onEmit(JSONObject... data) {
        Assert.fail(data[0].toString());
      }
    });
    agent.once("running",new Event<JSONObject>() {
      @Override
      public void onEmit(JSONObject... data) {
        System.out.println("state running");
        for(JSONObject d : data){
          System.out.println(d);
        }
        Assert.assertEquals(data.length,1);
        Assert.assertNotNull(data[0]);
        agent.on("running",new Event<JSONObject>() {
          @Override
          public void onEmit(JSONObject... data) {
            signal.countDown();
          }
        });
        agent.init(null);
      }
    });
    agent.once("null",new Event<JSONObject>() {
      @Override
      public void onEmit(JSONObject... data) {
        System.out.println("state null");
        JSONObject o;
        try {
          o = new JSONObject() {
            {
              put("data", new JSONObject());
            }
          };
          agent.send("signal", o, new Callback<String, String>() {
            
            @Override
            public void call(String error, String... args) {
              Assert.assertNull(error);
            }
          });
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
        
      }
    });
    connectEmitter.on("socket::connected",new Event<String>() {
      @Override
      public void onEmit(String... data) {
        agent.init(null);
      }
    });
    socket.connect();
    socket.subscribe("test.obj:state:running:"+agentId);
    socket.subscribe("test.obj:state:null:"+agentId);
    try {
      signal.await(2, TimeUnit.SECONDS);// wait for connect
      Assert.assertEquals("should have gotten to running",0, signal.getCount());
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    } finally {
      socket.disconnect();
    }
    
  }

}
