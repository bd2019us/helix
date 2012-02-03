package com.linkedin.helix.messaging.handling;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.linkedin.helix.ClusterManagerException;
import com.linkedin.helix.NotificationContext;
import com.linkedin.helix.messaging.AsyncCallback;
import com.linkedin.helix.messaging.handling.MessageHandler.ErrorCode;
import com.linkedin.helix.messaging.handling.MessageHandler.ErrorType;
import com.linkedin.helix.model.Message;
import com.linkedin.helix.model.Message.MessageType;
import com.linkedin.helix.participant.StateMachEngineImpl;

public class AsyncCallbackService implements MessageHandlerFactory
{
  private final ConcurrentHashMap<String, AsyncCallback> _callbackMap = new ConcurrentHashMap<String, AsyncCallback>();
  private static Logger _logger = Logger.getLogger(AsyncCallbackService.class);

  public AsyncCallbackService()
  {
  }

  public void registerAsyncCallback(String correlationId, AsyncCallback callback)
  {
    if (_callbackMap.containsKey(correlationId))
    {
      _logger.warn("correlation id " + correlationId + " already registered");
    }
    _logger.info("registering correlation id " + correlationId);
    _callbackMap.put(correlationId, callback);
  }

  void verifyMessage(Message message)
  {
    if (!message.getMsgType().toString()
        .equalsIgnoreCase(MessageType.TASK_REPLY.toString()))
    {
      String errorMsg = "Unexpected msg type for message " + message.getMsgId()
          + " type:" + message.getMsgType() + " Expected : "
          + MessageType.TASK_REPLY;
      _logger.error(errorMsg);
      throw new ClusterManagerException(errorMsg);
    }
    String correlationId = message.getCorrelationId();
    if (correlationId == null)
    {
      String errorMsg = "Message " + message.getMsgId()
          + " does not have correlation id";
      _logger.error(errorMsg);
      throw new ClusterManagerException(errorMsg);
    }

    if (!_callbackMap.containsKey(correlationId))
    {
      String errorMsg = "Message "
          + message.getMsgId()
          + " does not have correponding callback. Probably timed out already. Correlation id: "
          + correlationId;
      _logger.error(errorMsg);
      throw new ClusterManagerException(errorMsg);
    }
    _logger.info("Verified reply message " + message.getMsgId()
        + " correlation:" + correlationId);
  }

  @Override
  public MessageHandler createHandler(Message message,
      NotificationContext context)
  {
    verifyMessage(message);
    return new AsyncCallbackMessageHandler(message.getCorrelationId(),message, context);
  }

  @Override
  public String getMessageType()
  {
    return MessageType.TASK_REPLY.toString();
  }

  @Override
  public void reset()
  {

  }

  public class AsyncCallbackMessageHandler extends MessageHandler
  {
    private final String _correlationId;

    public AsyncCallbackMessageHandler(String correlationId, Message message, NotificationContext context)
    {
      super(message, context);
      _correlationId = correlationId;
    }

    @Override
    public CMTaskResult handleMessage() throws InterruptedException
    {
      verifyMessage(_message);
      CMTaskResult result = new CMTaskResult();
      assert (_correlationId.equalsIgnoreCase(_message.getCorrelationId()));
      _logger.info("invoking reply message " + _message.getMsgId()
          + ", correlationid:" + _correlationId);

      AsyncCallback callback = _callbackMap.get(_correlationId);
      synchronized (callback)
      {
        callback.onReply(_message);
        if (callback.isDone())
        {
          _logger.info("Removing finished callback, correlationid:"
              + _correlationId);
          _callbackMap.remove(_correlationId);
        }
      }
      result.setSuccess(true);
      return result;
    }

    @Override
    public void onError(Exception e, ErrorCode code, ErrorType type)
    {
      _logger.error("Message handling pipeline get an exception. MsgId:" + _message.getMsgId(), e);
    }
  }
}