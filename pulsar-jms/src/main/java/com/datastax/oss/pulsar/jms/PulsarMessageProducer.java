/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.pulsar.jms;

import javax.jms.CompletionListener;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.apache.pulsar.client.api.Producer;

class PulsarMessageProducer implements MessageProducer {
  private final PulsarSession session;
  private PulsarDestination defaultDestination;

  public PulsarMessageProducer(PulsarSession session, Destination defaultDestination)
      throws InvalidDestinationException {
    this.session = session;
    try {
      this.defaultDestination = (PulsarDestination) defaultDestination;
    } catch (ClassCastException err) {
      throw new InvalidDestinationException(
          "Invalid destination type " + defaultDestination.getClass());
    }
  }

  private boolean disableMessageId;
  private boolean disableMessageTimestamp;
  private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
  private int priority = Message.DEFAULT_PRIORITY;
  private long defaultTimeToLive = Message.DEFAULT_TIME_TO_LIVE;
  private long defaultDeliveryDelay = Message.DEFAULT_DELIVERY_DELAY;

  /**
   * Specify whether message IDs may be disabled.
   *
   * <p>Since message IDs take some effort to create and increase a message's size, some JMS
   * providers may be able to optimise message overhead if they are given a hint that the message ID
   * is not used by an application. By calling this method, a JMS application enables this potential
   * optimisation for all messages sent using this {@code MessageProducer}. If the JMS provider
   * accepts this hint, these messages must have the message ID set to null; if the provider ignores
   * the hint, the message ID must be set to its normal unique value.
   *
   * <p>Message IDs are enabled by default.
   *
   * @param value indicates if message IDs may be disabled
   * @throws JMSException if the JMS provider fails to set message ID to disabled due to some
   *     internal error.
   */
  @Override
  public void setDisableMessageID(boolean value) throws JMSException {
    this.disableMessageId = value;
  }

  /**
   * Gets an indication of whether message IDs are disabled.
   *
   * @return an indication of whether message IDs are disabled
   * @throws JMSException if the JMS provider fails to determine if message IDs are disabled due to
   *     some internal error.
   */
  @Override
  public boolean getDisableMessageID() throws JMSException {
    return disableMessageId;
  }

  /**
   * Specify whether message timestamps may be disabled.
   *
   * <p>Since timestamps take some effort to create and increase a message's size, some JMS
   * providers may be able to optimise message overhead if they are given a hint that the timestamp
   * is not used by an application. By calling this method, a JMS application enables this potential
   * optimisation for all messages sent using this {@code MessageProducer}. If the JMS provider
   * accepts this hint, these messages must have the timestamp set to zero; if the provider ignores
   * the hint, the timestamp must be set to its normal value.
   *
   * <p>Message timestamps are enabled by default.
   *
   * @param value indicates whether message timestamps may be disabled
   * @throws JMSException if the JMS provider fails to set timestamps to disabled due to some
   *     internal error.
   */
  @Override
  public void setDisableMessageTimestamp(boolean value) throws JMSException {
    this.disableMessageTimestamp = value;
  }

  /**
   * Gets an indication of whether message timestamps are disabled.
   *
   * @return an indication of whether message timestamps are disabled
   * @throws JMSException if the JMS provider fails to determine if timestamps are disabled due to
   *     some internal error.
   */
  @Override
  public boolean getDisableMessageTimestamp() throws JMSException {
    return disableMessageTimestamp;
  }

  /**
   * Sets the producer's default delivery mode.
   *
   * <p>Delivery mode is set to {@code PERSISTENT} by default.
   *
   * @param deliveryMode the message delivery mode for this message producer; legal values are
   *     {@code DeliveryMode.NON_PERSISTENT} and {@code DeliveryMode.PERSISTENT}
   * @throws JMSException if the JMS provider fails to set the delivery mode due to some internal
   *     error.
   * @see MessageProducer#getDeliveryMode
   * @see DeliveryMode#NON_PERSISTENT
   * @see DeliveryMode#PERSISTENT
   * @see Message#DEFAULT_DELIVERY_MODE
   */
  @Override
  public void setDeliveryMode(int deliveryMode) throws JMSException {
    this.deliveryMode = deliveryMode;
  }

  /**
   * Gets the producer's default delivery mode.
   *
   * @return the message delivery mode for this message producer
   * @throws JMSException if the JMS provider fails to get the delivery mode due to some internal
   *     error.
   * @see MessageProducer#setDeliveryMode
   */
  @Override
  public int getDeliveryMode() throws JMSException {
    return deliveryMode;
  }

  /**
   * Sets the producer's default priority.
   *
   * <p>The JMS API defines ten levels of priority value, with 0 as the lowest priority and 9 as the
   * highest. Clients should consider priorities 0-4 as gradations of normal priority and priorities
   * 5-9 as gradations of expedited priority. Priority is set to 4 by default.
   *
   * @param defaultPriority the message priority for this message producer; must be a value between
   *     0 and 9
   * @throws JMSException if the JMS provider fails to set the priority due to some internal error.
   * @see MessageProducer#getPriority
   * @see Message#DEFAULT_PRIORITY
   */
  @Override
  public void setPriority(int defaultPriority) throws JMSException {
    this.priority = priority;
  }

  /**
   * Gets the producer's default priority.
   *
   * @return the message priority for this message producer
   * @throws JMSException if the JMS provider fails to get the priority due to some internal error.
   * @see MessageProducer#setPriority
   */
  @Override
  public int getPriority() throws JMSException {
    return priority;
  }

  /**
   * Sets the default length of time in milliseconds from its dispatch time that a produced message
   * should be retained by the message system.
   *
   * <p>Time to live is set to zero by default.
   *
   * @param timeToLive the message time to live in milliseconds; zero is unlimited
   * @throws JMSException if the JMS provider fails to set the time to live due to some internal
   *     error.
   * @see MessageProducer#getTimeToLive
   * @see Message#DEFAULT_TIME_TO_LIVE
   */
  @Override
  public void setTimeToLive(long timeToLive) throws JMSException {
    this.defaultTimeToLive = timeToLive;
  }

  /**
   * Gets the default length of time in milliseconds from its dispatch time that a produced message
   * should be retained by the message system.
   *
   * @return the message time to live in milliseconds; zero is unlimited
   * @throws JMSException if the JMS provider fails to get the time to live due to some internal
   *     error.
   * @see MessageProducer#setTimeToLive
   */
  @Override
  public long getTimeToLive() throws JMSException {
    return defaultTimeToLive;
  }

  /**
   * Sets the minimum length of time in milliseconds that must elapse after a message is sent before
   * the JMS provider may deliver the message to a consumer.
   *
   * <p>For transacted sends, this time starts when the client sends the message, not when the
   * transaction is committed.
   *
   * <p>deliveryDelay is set to zero by default.
   *
   * @param deliveryDelay the delivery delay in milliseconds.
   * @throws JMSException if the JMS provider fails to set the delivery delay due to some internal
   *     error.
   * @see MessageProducer#getDeliveryDelay
   * @see Message#DEFAULT_DELIVERY_DELAY
   * @since JMS 2.0
   */
  @Override
  public void setDeliveryDelay(long deliveryDelay) throws JMSException {
    this.defaultDeliveryDelay = deliveryDelay;
  }

  /**
   * Gets the minimum length of time in milliseconds that must elapse after a message is sent before
   * the JMS provider may deliver the message to a consumer.
   *
   * @return the delivery delay in milliseconds.
   * @throws JMSException if the JMS provider fails to get the delivery delay due to some internal
   *     error.
   * @see MessageProducer#setDeliveryDelay
   * @since JMS 2.0
   */
  @Override
  public long getDeliveryDelay() throws JMSException {
    return defaultDeliveryDelay;
  }

  /**
   * Gets the destination associated with this {@code MessageProducer}.
   *
   * @return this producer's {@code Destination}
   * @throws JMSException if the JMS provider fails to get the destination for this {@code
   *     MessageProducer} due to some internal error.
   * @since JMS 1.1
   */
  @Override
  public Destination getDestination() throws JMSException {
    return defaultDestination;
  }

  /**
   * Closes the message producer.
   *
   * <p>Since a provider may allocate some resources on behalf of a {@code MessageProducer} outside
   * the Java virtual machine, clients should close them when they are not needed. Relying on
   * garbage collection to eventually reclaim these resources may not be timely enough.
   *
   * <p>This method must not return until any incomplete asynchronous send operations for this
   * <tt>MessageProducer</tt> have been completed and any <tt>CompletionListener</tt> callbacks have
   * returned. Incomplete sends should be allowed to complete normally unless an error occurs.
   *
   * <p>A <tt>CompletionListener</tt> callback method must not call <tt>close</tt> on its own
   * <tt>MessageProducer</tt>. Doing so will cause an <tt>IllegalStateException</tt> to be thrown.
   *
   * @throws IllegalStateException this method has been called by a <tt>CompletionListener</tt>
   *     callback method on its own <tt>MessageProducer</tt>
   * @throws JMSException if the JMS provider fails to close the producer due to some internal
   *     error.
   */
  @Override
  public void close() throws JMSException {}

  /**
   * Sends a message using the {@code MessageProducer}'s default delivery mode, priority, and time
   * to live.
   *
   * @param message the message to send
   * @throws JMSException if the JMS provider fails to send the message due to some internal error.
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with a {@code MessageProducer}
   *     with an invalid destination.
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that did not specify a destination at creation time.
   * @see Session#createProducer
   * @since JMS 1.1
   */
  @Override
  public void send(Message message) throws JMSException {
    validateMessageSend(defaultDestination, 0);
    sendMessage(defaultDestination, message);
  }

  /**
   * Sends a message, specifying delivery mode, priority, and time to live.
   *
   * @param message the message to send
   * @param deliveryMode the delivery mode to use
   * @param priority the priority for this message
   * @param timeToLive the message's lifetime (in milliseconds)
   * @throws JMSException if the JMS provider fails to send the message due to some internal error.
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with a {@code MessageProducer}
   *     with an invalid destination.
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that did not specify a destination at creation time.
   * @see Session#createProducer
   * @since JMS 1.1
   */
  @Override
  public void send(Message message, int deliveryMode, int priority, long timeToLive)
      throws JMSException {
    validateMessageSend(defaultDestination, timeToLive);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSPriority(priority);
    sendMessage(defaultDestination, message);
  }

  /**
   * Sends a message to a destination for an unidentified message producer using the {@code
   * MessageProducer}'s default delivery mode, priority, and time to live.
   *
   * <p>Typically, a message producer is assigned a destination at creation time; however, the JMS
   * API also supports unidentified message producers, which require that the destination be
   * supplied every time a message is sent.
   *
   * @param destination the destination to send this message to
   * @param message the message to send
   * @throws JMSException if the JMS provider fails to send the message due to some internal error.
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with an invalid destination.
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that specified a destination at creation time.
   * @see Session#createProducer
   * @since JMS 1.1
   */
  @Override
  public void send(Destination destination, Message message) throws JMSException {
    validateMessageSend(destination, 0);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSPriority(priority);
    sendMessage(destination, message);
  }

  /**
   * Sends a message to a destination for an unidentified message producer, specifying delivery
   * mode, priority and time to live.
   *
   * <p>Typically, a message producer is assigned a destination at creation time; however, the JMS
   * API also supports unidentified message producers, which require that the destination be
   * supplied every time a message is sent.
   *
   * @param destination the destination to send this message to
   * @param message the message to send
   * @param deliveryMode the delivery mode to use
   * @param priority the priority for this message
   * @param timeToLive the message's lifetime (in milliseconds)
   * @throws JMSException if the JMS provider fails to send the message due to some internal error.
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with an invalid destination.
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that specified a destination at creation time.
   * @see Session#createProducer
   * @since JMS 1.1
   */
  @Override
  public void send(
      Destination destination, Message message, int deliveryMode, int priority, long timeToLive)
      throws JMSException {
    validateMessageSend(destination, timeToLive);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSPriority(priority);
    sendMessage(destination, message);
  }

  private void validateMessageSend(Destination destination, long timeToLive) throws JMSException {
    if (destination == null) {
      throw new UnsupportedOperationException("destination is null");
    }
    if (timeToLive > 0) {
      throw new JMSException("timeToLive not supported");
    }
  }

  /**
   * Sends a message using the {@code MessageProducer}'s default delivery mode, priority, and time
   * to live, performing part of the work involved in sending the message in a separate thread and
   * notifying the specified <tt>CompletionListener</tt> when the operation has completed. JMS
   * refers to this as an "asynchronous send".
   *
   * <p>When the message has been successfully sent the JMS provider invokes the callback method
   * <tt>onCompletion</tt> on an application-specified <tt>CompletionListener</tt> object. Only when
   * that callback has been invoked can the application be sure that the message has been
   * successfully sent with the same degree of confidence as if a normal synchronous send had been
   * performed. An application which requires this degree of confidence must therefore wait for the
   * callback to be invoked before continuing.
   *
   * <p>The following information is intended to give an indication of how an asynchronous send
   * would typically be implemented.
   *
   * <p>In some JMS providers, a normal synchronous send involves sending the message to a remote
   * JMS server and then waiting for an acknowledgement to be received before returning. It is
   * expected that such a provider would implement an asynchronous send by sending the message to
   * the remote JMS server and then returning without waiting for an acknowledgement. When the
   * acknowledgement is received, the JMS provider would notify the application by invoking the
   * <tt>onCompletion</tt> method on the application-specified <tt>CompletionListener</tt> object.
   * If for some reason the acknowledgement is not received the JMS provider would notify the
   * application by invoking the <tt>CompletionListener</tt>'s <tt>onException</tt> method.
   *
   * <p>In those cases where the JMS specification permits a lower level of reliability, a normal
   * synchronous send might not wait for an acknowledgement. In that case it is expected that an
   * asynchronous send would be similar to a synchronous send: the JMS provider would send the
   * message to the remote JMS server and then return without waiting for an acknowledgement.
   * However the JMS provider would still notify the application that the send had completed by
   * invoking the <tt>onCompletion</tt> method on the application-specified
   * <tt>CompletionListener</tt> object.
   *
   * <p>It is up to the JMS provider to decide exactly what is performed in the calling thread and
   * what, if anything, is performed asynchronously, so long as it satisfies the requirements given
   * below:
   *
   * <p><b>Quality of service</b>: After the send operation has completed successfully, which means
   * that the message has been successfully sent with the same degree of confidence as if a normal
   * synchronous send had been performed, the JMS provider must invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> method. The <tt>CompletionListener</tt>
   * must not be invoked earlier than this.
   *
   * <p><b>Exceptions</b>: If an exception is encountered during the call to the <tt>send</tt>
   * method then an appropriate exception should be thrown in the thread that is calling the
   * <tt>send</tt> method. In this case the JMS provider must not invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method. If an
   * exception is encountered which cannot be thrown in the thread that is calling the <tt>send</tt>
   * method then the JMS provider must call the <tt>CompletionListener</tt>'s <tt>onException</tt>
   * method. In both cases if an exception occurs it is undefined whether or not the message was
   * successfully sent.
   *
   * <p><b>Message order</b>: If the same <tt>MessageProducer</tt> is used to send multiple messages
   * then JMS message ordering requirements must be satisfied. This applies even if a combination of
   * synchronous and asynchronous sends has been performed. The application is not required to wait
   * for an asynchronous send to complete before sending the next message.
   *
   * <p><b>Close, commit or rollback</b>: If the <tt>close</tt> method is called on the
   * <tt>MessageProducer</tt> or its <tt>Session</tt> or <tt>Connection</tt> then the JMS provider
   * must block until any incomplete send operations have been completed and all {@code
   * CompletionListener} callbacks have returned before closing the object and returning. If the
   * session is transacted (uses a local transaction) then when the <tt>Session</tt>'s
   * <tt>commit</tt> or <tt>rollback</tt> method is called the JMS provider must block until any
   * incomplete send operations have been completed and all {@code CompletionListener} callbacks
   * have returned before performing the commit or rollback. Incomplete sends should be allowed to
   * complete normally unless an error occurs.
   *
   * <p>A <tt>CompletionListener</tt> callback method must not call <tt>close</tt> on its own
   * <tt>Connection</tt>, <tt>Session</tt> or <tt>MessageProducer</tt> or call <tt>commit</tt> or
   * <tt>rollback</tt> on its own <tt>Session</tt>. Doing so will cause the <tt>close</tt>,
   * <tt>commit</tt> or <tt>rollback</tt> to throw an <tt>IllegalStateException</tt>.
   *
   * <p><b>Restrictions on usage in Java EE</b> This method must not be used in a Java EE EJB or web
   * container. Doing so may cause a {@code JMSException} to be thrown though this is not
   * guaranteed.
   *
   * <p><b>Message headers</b> JMS defines a number of message header fields and message properties
   * which must be set by the "JMS provider on send". If the send is asynchronous these fields and
   * properties may be accessed on the sending client only after the <tt>CompletionListener</tt> has
   * been invoked. If the <tt>CompletionListener</tt>'s <tt>onException</tt> method is called then
   * the state of these message header fields and properties is undefined.
   *
   * <p><b>Restrictions on threading</b>: Applications that perform an asynchronous send must
   * confirm to the threading restrictions defined in JMS. This means that the session may be used
   * by only one thread at a time.
   *
   * <p>Setting a <tt>CompletionListener</tt> does not cause the session to be dedicated to the
   * thread of control which calls the <tt>CompletionListener</tt>. The application thread may
   * therefore continue to use the session after performing an asynchronous send. However the
   * <tt>CompletionListener</tt>'s callback methods must not use the session if an application
   * thread might be using the session at the same time.
   *
   * <p><b>Use of the <tt>CompletionListener</tt> by the JMS provider</b>: A session will only
   * invoke one <tt>CompletionListener</tt> callback method at a time. For a given
   * <tt>MessageProducer</tt>, callbacks (both {@code onCompletion} and {@code onException}) will be
   * performed in the same order as the corresponding calls to the asynchronous send method. A JMS
   * provider must not invoke the <tt>CompletionListener</tt> from the thread that is calling the
   * asynchronous <tt>send</tt> method.
   *
   * <p><b>Restrictions on the use of the Message object</b>: Applications which perform an
   * asynchronous send must take account of the restriction that a <tt>Message</tt> object is
   * designed to be accessed by one logical thread of control at a time and does not support
   * concurrent use.
   *
   * <p>After the <tt>send</tt> method has returned, the application must not attempt to read the
   * headers, properties or body of the <tt>Message</tt> object until the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method has been
   * called. This is because the JMS provider may be modifying the <tt>Message</tt> object in
   * another thread during this time. The JMS provider may throw an <tt>JMSException</tt> if the
   * application attempts to access or modify the <tt>Message</tt> object after the <tt>send</tt>
   * method has returned and before the <tt>CompletionListener</tt> has been invoked. If the JMS
   * provider does not throw an exception then the behaviour is undefined.
   *
   * @param message the message to send
   * @param completionListener a {@code CompletionListener} to be notified when the send has
   *     completed
   * @throws JMSException if an internal error occurs
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with a {@code MessageProducer}
   *     with an invalid destination.
   * @throws IllegalArgumentException if the specified {@code CompletionListener} is null
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that did not specify a destination at creation time.
   * @see Session#createProducer
   * @see CompletionListener
   * @since JMS 2.0
   */
  @Override
  public void send(Message message, CompletionListener completionListener) throws JMSException {
    validateMessageSend(defaultDestination, 0);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSPriority(priority);
    sendMessage(defaultDestination, message, completionListener);
  }

  /**
   * Sends a message, specifying delivery mode, priority and time to live, performing part of the
   * work involved in sending the message in a separate thread and notifying the specified
   * <tt>CompletionListener</tt> when the operation has completed. JMS refers to this as an
   * "asynchronous send".
   *
   * <p>When the message has been successfully sent the JMS provider invokes the callback method
   * <tt>onCompletion</tt> on an application-specified <tt>CompletionListener</tt> object. Only when
   * that callback has been invoked can the application be sure that the message has been
   * successfully sent with the same degree of confidence as if a normal synchronous send had been
   * performed. An application which requires this degree of confidence must therefore wait for the
   * callback to be invoked before continuing.
   *
   * <p>The following information is intended to give an indication of how an asynchronous send
   * would typically be implemented.
   *
   * <p>In some JMS providers, a normal synchronous send involves sending the message to a remote
   * JMS server and then waiting for an acknowledgement to be received before returning. It is
   * expected that such a provider would implement an asynchronous send by sending the message to
   * the remote JMS server and then returning without waiting for an acknowledgement. When the
   * acknowledgement is received, the JMS provider would notify the application by invoking the
   * <tt>onCompletion</tt> method on the application-specified <tt>CompletionListener</tt> object.
   * If for some reason the acknowledgement is not received the JMS provider would notify the
   * application by invoking the <tt>CompletionListener</tt>'s <tt>onException</tt> method.
   *
   * <p>In those cases where the JMS specification permits a lower level of reliability, a normal
   * synchronous send might not wait for an acknowledgement. In that case it is expected that an
   * asynchronous send would be similar to a synchronous send: the JMS provider would send the
   * message to the remote JMS server and then return without waiting for an acknowledgement.
   * However the JMS provider would still notify the application that the send had completed by
   * invoking the <tt>onCompletion</tt> method on the application-specified
   * <tt>CompletionListener</tt> object.
   *
   * <p>It is up to the JMS provider to decide exactly what is performed in the calling thread and
   * what, if anything, is performed asynchronously, so long as it satisfies the requirements given
   * below:
   *
   * <p><b>Quality of service</b>: After the send operation has completed successfully, which means
   * that the message has been successfully sent with the same degree of confidence as if a normal
   * synchronous send had been performed, the JMS provider must invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> method. The <tt>CompletionListener</tt>
   * must not be invoked earlier than this.
   *
   * <p><b>Exceptions</b>: If an exception is encountered during the call to the <tt>send</tt>
   * method then an appropriate exception should be thrown in the thread that is calling the
   * <tt>send</tt> method. In this case the JMS provider must not invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method. If an
   * exception is encountered which cannot be thrown in the thread that is calling the <tt>send</tt>
   * method then the JMS provider must call the <tt>CompletionListener</tt>'s <tt>onException</tt>
   * method. In both cases if an exception occurs it is undefined whether or not the message was
   * successfully sent.
   *
   * <p><b>Message order</b>: If the same <tt>MessageProducer</tt> is used to send multiple messages
   * then JMS message ordering requirements must be satisfied. This applies even if a combination of
   * synchronous and asynchronous sends has been performed. The application is not required to wait
   * for an asynchronous send to complete before sending the next message.
   *
   * <p><b>Close, commit or rollback</b>: If the <tt>close</tt> method is called on the
   * <tt>MessageProducer</tt> or its <tt>Session</tt> or <tt>Connection</tt> then the JMS provider
   * must block until any incomplete send operations have been completed and all {@code
   * CompletionListener} callbacks have returned before closing the object and returning. If the
   * session is transacted (uses a local transaction) then when the <tt>Session</tt>'s
   * <tt>commit</tt> or <tt>rollback</tt> method is called the JMS provider must block until any
   * incomplete send operations have been completed and all {@code CompletionListener} callbacks
   * have returned before performing the commit or rollback. Incomplete sends should be allowed to
   * complete normally unless an error occurs.
   *
   * <p>A <tt>CompletionListener</tt> callback method must not call <tt>close</tt> on its own
   * <tt>Connection</tt>, <tt>Session</tt> or <tt>MessageProducer</tt> or call <tt>commit</tt> or
   * <tt>rollback</tt> on its own <tt>Session</tt>. Doing so will cause the <tt>close</tt>,
   * <tt>commit</tt> or <tt>rollback</tt> to throw an <tt>IllegalStateException</tt>.
   *
   * <p><b>Restrictions on usage in Java EE</b> This method must not be used in a Java EE EJB or web
   * container. Doing so may cause a {@code JMSException} to be thrown though this is not
   * guaranteed.
   *
   * <p><b>Message headers</b> JMS defines a number of message header fields and message properties
   * which must be set by the "JMS provider on send". If the send is asynchronous these fields and
   * properties may be accessed on the sending client only after the <tt>CompletionListener</tt> has
   * been invoked. If the <tt>CompletionListener</tt>'s <tt>onException</tt> method is called then
   * the state of these message header fields and properties is undefined.
   *
   * <p><b>Restrictions on threading</b>: Applications that perform an asynchronous send must
   * confirm to the threading restrictions defined in JMS. This means that the session may be used
   * by only one thread at a time.
   *
   * <p>Setting a <tt>CompletionListener</tt> does not cause the session to be dedicated to the
   * thread of control which calls the <tt>CompletionListener</tt>. The application thread may
   * therefore continue to use the session after performing an asynchronous send. However the
   * <tt>CompletionListener</tt>'s callback methods must not use the session if an application
   * thread might be using the session at the same time.
   *
   * <p><b>Use of the <tt>CompletionListener</tt> by the JMS provider</b>: A session will only
   * invoke one <tt>CompletionListener</tt> callback method at a time. For a given
   * <tt>MessageProducer</tt>, callbacks (both {@code onCompletion} and {@code onException}) will be
   * performed in the same order as the corresponding calls to the asynchronous send method. A JMS
   * provider must not invoke the <tt>CompletionListener</tt> from the thread that is calling the
   * asynchronous <tt>send</tt> method.
   *
   * <p><b>Restrictions on the use of the Message object</b>: Applications which perform an
   * asynchronous send must take account of the restriction that a <tt>Message</tt> object is
   * designed to be accessed by one logical thread of control at a time and does not support
   * concurrent use.
   *
   * <p>After the <tt>send</tt> method has returned, the application must not attempt to read the
   * headers, properties or body of the <tt>Message</tt> object until the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method has been
   * called. This is because the JMS provider may be modifying the <tt>Message</tt> object in
   * another thread during this time. The JMS provider may throw an <tt>JMSException</tt> if the
   * application attempts to access or modify the <tt>Message</tt> object after the <tt>send</tt>
   * method has returned and before the <tt>CompletionListener</tt> has been invoked. If the JMS
   * provider does not throw an exception then the behaviour is undefined.
   *
   * @param message the message to send
   * @param deliveryMode the delivery mode to use
   * @param priority the priority for this message
   * @param timeToLive the message's lifetime (in milliseconds)
   * @param completionListener a {@code CompletionListener} to be notified when the send has
   *     completed
   * @throws JMSException if an internal error occurs
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with a {@code MessageProducer}
   *     with an invalid destination.
   * @throws IllegalArgumentException if the specified {@code CompletionListener} is null
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that did not specify a destination at creation time.
   * @see Session#createProducer
   * @see CompletionListener
   * @since JMS 2.0
   */
  @Override
  public void send(
      Message message,
      int deliveryMode,
      int priority,
      long timeToLive,
      CompletionListener completionListener)
      throws JMSException {
    validateMessageSend(defaultDestination, timeToLive);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSPriority(priority);
    sendMessage(defaultDestination, message, completionListener);
  }

  /**
   * Sends a message to a destination for an unidentified message producer, using the {@code
   * MessageProducer}'s default delivery mode, priority, and time to live, performing part of the
   * work involved in sending the message in a separate thread and notifying the specified
   * <tt>CompletionListener</tt> when the operation has completed. JMS refers to this as an
   * "asynchronous send".
   *
   * <p>Typically, a message producer is assigned a destination at creation time; however, the JMS
   * API also supports unidentified message producers, which require that the destination be
   * supplied every time a message is sent.
   *
   * <p>When the message has been successfully sent the JMS provider invokes the callback method
   * <tt>onCompletion</tt> on an application-specified <tt>CompletionListener</tt> object. Only when
   * that callback has been invoked can the application be sure that the message has been
   * successfully sent with the same degree of confidence as if a normal synchronous send had been
   * performed. An application which requires this degree of confidence must therefore wait for the
   * callback to be invoked before continuing.
   *
   * <p>The following information is intended to give an indication of how an asynchronous send
   * would typically be implemented.
   *
   * <p>In some JMS providers, a normal synchronous send involves sending the message to a remote
   * JMS server and then waiting for an acknowledgement to be received before returning. It is
   * expected that such a provider would implement an asynchronous send by sending the message to
   * the remote JMS server and then returning without waiting for an acknowledgement. When the
   * acknowledgement is received, the JMS provider would notify the application by invoking the
   * <tt>onCompletion</tt> method on the application-specified <tt>CompletionListener</tt> object.
   * If for some reason the acknowledgement is not received the JMS provider would notify the
   * application by invoking the <tt>CompletionListener</tt>'s <tt>onException</tt> method.
   *
   * <p>In those cases where the JMS specification permits a lower level of reliability, a normal
   * synchronous send might not wait for an acknowledgement. In that case it is expected that an
   * asynchronous send would be similar to a synchronous send: the JMS provider would send the
   * message to the remote JMS server and then return without waiting for an acknowledgement.
   * However the JMS provider would still notify the application that the send had completed by
   * invoking the <tt>onCompletion</tt> method on the application-specified
   * <tt>CompletionListener</tt> object.
   *
   * <p>It is up to the JMS provider to decide exactly what is performed in the calling thread and
   * what, if anything, is performed asynchronously, so long as it satisfies the requirements given
   * below:
   *
   * <p><b>Quality of service</b>: After the send operation has completed successfully, which means
   * that the message has been successfully sent with the same degree of confidence as if a normal
   * synchronous send had been performed, the JMS provider must invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> method. The <tt>CompletionListener</tt>
   * must not be invoked earlier than this.
   *
   * <p><b>Exceptions</b>: If an exception is encountered during the call to the <tt>send</tt>
   * method then an appropriate exception should be thrown in the thread that is calling the
   * <tt>send</tt> method. In this case the JMS provider must not invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method. If an
   * exception is encountered which cannot be thrown in the thread that is calling the <tt>send</tt>
   * method then the JMS provider must call the <tt>CompletionListener</tt>'s <tt>onException</tt>
   * method. In both cases if an exception occurs it is undefined whether or not the message was
   * successfully sent.
   *
   * <p><b>Message order</b>: If the same <tt>MessageProducer</tt> is used to send multiple messages
   * then JMS message ordering requirements must be satisfied. This applies even if a combination of
   * synchronous and asynchronous sends has been performed. The application is not required to wait
   * for an asynchronous send to complete before sending the next message.
   *
   * <p><b>Close, commit or rollback</b>: If the <tt>close</tt> method is called on the
   * <tt>MessageProducer</tt> or its <tt>Session</tt> or <tt>Connection</tt> then the JMS provider
   * must block until any incomplete send operations have been completed and all {@code
   * CompletionListener} callbacks have returned before closing the object and returning. If the
   * session is transacted (uses a local transaction) then when the <tt>Session</tt>'s
   * <tt>commit</tt> or <tt>rollback</tt> method is called the JMS provider must block until any
   * incomplete send operations have been completed and all {@code CompletionListener} callbacks
   * have returned before performing the commit or rollback. Incomplete sends should be allowed to
   * complete normally unless an error occurs.
   *
   * <p>A <tt>CompletionListener</tt> callback method must not call <tt>close</tt> on its own
   * <tt>Connection</tt>, <tt>Session</tt> or <tt>MessageProducer</tt> or call <tt>commit</tt> or
   * <tt>rollback</tt> on its own <tt>Session</tt>. Doing so will cause the <tt>close</tt>,
   * <tt>commit</tt> or <tt>rollback</tt> to throw an <tt>IllegalStateException</tt>.
   *
   * <p><b>Restrictions on usage in Java EE</b> This method must not be used in a Java EE EJB or web
   * container. Doing so may cause a {@code JMSException} to be thrown though this is not
   * guaranteed.
   *
   * <p><b>Message headers</b> JMS defines a number of message header fields and message properties
   * which must be set by the "JMS provider on send". If the send is asynchronous these fields and
   * properties may be accessed on the sending client only after the <tt>CompletionListener</tt> has
   * been invoked. If the <tt>CompletionListener</tt>'s <tt>onException</tt> method is called then
   * the state of these message header fields and properties is undefined.
   *
   * <p><b>Restrictions on threading</b>: Applications that perform an asynchronous send must
   * confirm to the threading restrictions defined in JMS. This means that the session may be used
   * by only one thread at a time.
   *
   * <p>Setting a <tt>CompletionListener</tt> does not cause the session to be dedicated to the
   * thread of control which calls the <tt>CompletionListener</tt>. The application thread may
   * therefore continue to use the session after performing an asynchronous send. However the
   * <tt>CompletionListener</tt>'s callback methods must not use the session if an application
   * thread might be using the session at the same time.
   *
   * <p><b>Use of the <tt>CompletionListener</tt> by the JMS provider</b>: A session will only
   * invoke one <tt>CompletionListener</tt> callback method at a time. For a given
   * <tt>MessageProducer</tt>, callbacks (both {@code onCompletion} and {@code onException}) will be
   * performed in the same order as the corresponding calls to the asynchronous send method. A JMS
   * provider must not invoke the <tt>CompletionListener</tt> from the thread that is calling the
   * asynchronous <tt>send</tt> method.
   *
   * <p><b>Restrictions on the use of the Message object</b>: Applications which perform an
   * asynchronous send must take account of the restriction that a <tt>Message</tt> object is
   * designed to be accessed by one logical thread of control at a time and does not support
   * concurrent use.
   *
   * <p>After the <tt>send</tt> method has returned, the application must not attempt to read the
   * headers, properties or body of the <tt>Message</tt> object until the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method has been
   * called. This is because the JMS provider may be modifying the <tt>Message</tt> object in
   * another thread during this time. The JMS provider may throw an <tt>JMSException</tt> if the
   * application attempts to access or modify the <tt>Message</tt> object after the <tt>send</tt>
   * method has returned and before the <tt>CompletionListener</tt> has been invoked. If the JMS
   * provider does not throw an exception then the behaviour is undefined.
   *
   * @param destination the destination to send this message to
   * @param message the message to send
   * @param completionListener a {@code CompletionListener} to be notified when the send has
   *     completed
   * @throws JMSException if an internal error occurs
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with an invalid destination
   * @throws IllegalArgumentException if the specified {@code CompletionListener} is null
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that specified a destination at creation time.
   * @see Session#createProducer
   * @see CompletionListener
   * @since JMS 2.0
   */
  @Override
  public void send(Destination destination, Message message, CompletionListener completionListener)
      throws JMSException {
    validateMessageSend(destination, 0);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSPriority(priority);
    sendMessage(destination, message, completionListener);
  }

  /**
   * Sends a message to a destination for an unidentified message producer, specifying delivery
   * mode, priority and time to live, performing part of the work involved in sending the message in
   * a separate thread and notifying the specified <tt>CompletionListener</tt> when the operation
   * has completed. JMS refers to this as an "asynchronous send".
   *
   * <p>Typically, a message producer is assigned a destination at creation time; however, the JMS
   * API also supports unidentified message producers, which require that the destination be
   * supplied every time a message is sent.
   *
   * <p>When the message has been successfully sent the JMS provider invokes the callback method
   * <tt>onCompletion</tt> on an application-specified <tt>CompletionListener</tt> object. Only when
   * that callback has been invoked can the application be sure that the message has been
   * successfully sent with the same degree of confidence as if a normal synchronous send had been
   * performed. An application which requires this degree of confidence must therefore wait for the
   * callback to be invoked before continuing.
   *
   * <p>The following information is intended to give an indication of how an asynchronous send
   * would typically be implemented.
   *
   * <p>In some JMS providers, a normal synchronous send involves sending the message to a remote
   * JMS server and then waiting for an acknowledgement to be received before returning. It is
   * expected that such a provider would implement an asynchronous send by sending the message to
   * the remote JMS server and then returning without waiting for an acknowledgement. When the
   * acknowledgement is received, the JMS provider would notify the application by invoking the
   * <tt>onCompletion</tt> method on the application-specified <tt>CompletionListener</tt> object.
   * If for some reason the acknowledgement is not received the JMS provider would notify the
   * application by invoking the <tt>CompletionListener</tt>'s <tt>onException</tt> method.
   *
   * <p>In those cases where the JMS specification permits a lower level of reliability, a normal
   * synchronous send might not wait for an acknowledgement. In that case it is expected that an
   * asynchronous send would be similar to a synchronous send: the JMS provider would send the
   * message to the remote JMS server and then return without waiting for an acknowledgement.
   * However the JMS provider would still notify the application that the send had completed by
   * invoking the <tt>onCompletion</tt> method on the application-specified
   * <tt>CompletionListener</tt> object.
   *
   * <p>It is up to the JMS provider to decide exactly what is performed in the calling thread and
   * what, if anything, is performed asynchronously, so long as it satisfies the requirements given
   * below:
   *
   * <p><b>Quality of service</b>: After the send operation has completed successfully, which means
   * that the message has been successfully sent with the same degree of confidence as if a normal
   * synchronous send had been performed, the JMS provider must invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> method. The <tt>CompletionListener</tt>
   * must not be invoked earlier than this.
   *
   * <p><b>Exceptions</b>: If an exception is encountered during the call to the <tt>send</tt>
   * method then an appropriate exception should be thrown in the thread that is calling the
   * <tt>send</tt> method. In this case the JMS provider must not invoke the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method. If an
   * exception is encountered which cannot be thrown in the thread that is calling the <tt>send</tt>
   * method then the JMS provider must call the <tt>CompletionListener</tt>'s <tt>onException</tt>
   * method. In both cases if an exception occurs it is undefined whether or not the message was
   * successfully sent.
   *
   * <p><b>Message order</b>: If the same <tt>MessageProducer</tt> is used to send multiple messages
   * then JMS message ordering requirements must be satisfied. This applies even if a combination of
   * synchronous and asynchronous sends has been performed. The application is not required to wait
   * for an asynchronous send to complete before sending the next message.
   *
   * <p><b>Close, commit or rollback</b>: If the <tt>close</tt> method is called on the
   * <tt>MessageProducer</tt> or its <tt>Session</tt> or <tt>Connection</tt> then the JMS provider
   * must block until any incomplete send operations have been completed and all {@code
   * CompletionListener} callbacks have returned before closing the object and returning. If the
   * session is transacted (uses a local transaction) then when the <tt>Session</tt>'s
   * <tt>commit</tt> or <tt>rollback</tt> method is called the JMS provider must block until any
   * incomplete send operations have been completed and all {@code CompletionListener} callbacks
   * have returned before performing the commit or rollback. Incomplete sends should be allowed to
   * complete normally unless an error occurs.
   *
   * <p>A <tt>CompletionListener</tt> callback method must not call <tt>close</tt> on its own
   * <tt>Connection</tt>, <tt>Session</tt> or <tt>MessageProducer</tt> or call <tt>commit</tt> or
   * <tt>rollback</tt> on its own <tt>Session</tt>. Doing so will cause the <tt>close</tt>,
   * <tt>commit</tt> or <tt>rollback</tt> to throw an <tt>IllegalStateException</tt>.
   *
   * <p><b>Restrictions on usage in Java EE</b> This method must not be used in a Java EE EJB or web
   * container. Doing so may cause a {@code JMSException} to be thrown though this is not
   * guaranteed.
   *
   * <p><b>Message headers</b> JMS defines a number of message header fields and message properties
   * which must be set by the "JMS provider on send". If the send is asynchronous these fields and
   * properties may be accessed on the sending client only after the <tt>CompletionListener</tt> has
   * been invoked. If the <tt>CompletionListener</tt>'s <tt>onException</tt> method is called then
   * the state of these message header fields and properties is undefined.
   *
   * <p><b>Restrictions on threading</b>: Applications that perform an asynchronous send must
   * confirm to the threading restrictions defined in JMS. This means that the session may be used
   * by only one thread at a time.
   *
   * <p>Setting a <tt>CompletionListener</tt> does not cause the session to be dedicated to the
   * thread of control which calls the <tt>CompletionListener</tt>. The application thread may
   * therefore continue to use the session after performing an asynchronous send. However the
   * <tt>CompletionListener</tt>'s callback methods must not use the session if an application
   * thread might be using the session at the same time.
   *
   * <p><b>Use of the <tt>CompletionListener</tt> by the JMS provider</b>: A session will only
   * invoke one <tt>CompletionListener</tt> callback method at a time. For a given
   * <tt>MessageProducer</tt>, callbacks (both {@code onCompletion} and {@code onException}) will be
   * performed in the same order as the corresponding calls to the asynchronous send method. A JMS
   * provider must not invoke the <tt>CompletionListener</tt> from the thread that is calling the
   * asynchronous <tt>send</tt> method.
   *
   * <p><b>Restrictions on the use of the Message object</b>: Applications which perform an
   * asynchronous send must take account of the restriction that a <tt>Message</tt> object is
   * designed to be accessed by one logical thread of control at a time and does not support
   * concurrent use.
   *
   * <p>After the <tt>send</tt> method has returned, the application must not attempt to read the
   * headers, properties or body of the <tt>Message</tt> object until the
   * <tt>CompletionListener</tt>'s <tt>onCompletion</tt> or <tt>onException</tt> method has been
   * called. This is because the JMS provider may be modifying the <tt>Message</tt> object in
   * another thread during this time. The JMS provider may throw an <tt>JMSException</tt> if the
   * application attempts to access or modify the <tt>Message</tt> object after the <tt>send</tt>
   * method has returned and before the <tt>CompletionListener</tt> has been invoked. If the JMS
   * provider does not throw an exception then the behaviour is undefined.
   *
   * @param destination the destination to send this message to
   * @param message the message to send
   * @param deliveryMode the delivery mode to use
   * @param priority the priority for this message
   * @param timeToLive the message's lifetime (in milliseconds)
   * @param completionListener a {@code CompletionListener} to be notified when the send has
   *     completed
   * @throws JMSException if an internal error occurs
   * @throws MessageFormatException if an invalid message is specified.
   * @throws InvalidDestinationException if a client uses this method with an invalid destination.
   * @throws IllegalArgumentException if the specified {@code CompletionListener} is null
   * @throws UnsupportedOperationException if a client uses this method with a {@code
   *     MessageProducer} that specified a destination at creation time.
   * @see Session#createProducer
   * @see CompletionListener
   * @since JMS 2.0
   */
  @Override
  public void send(
      Destination destination,
      Message message,
      int deliveryMode,
      int priority,
      long timeToLive,
      CompletionListener completionListener)
      throws JMSException {
    validateMessageSend(destination, 0);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    message.setJMSDeliveryMode(deliveryMode);
    message.setJMSPriority(priority);
    sendMessage(destination, message, completionListener);
  }

  private void sendMessage(Destination defaultDestination, Message message) throws JMSException {
    Producer<byte[]> producer =
        session.getFactory().getProducerForDestination((PulsarDestination) defaultDestination);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    if (session.transaction != null) {
      pulsarMessage.send(producer.newMessage(session.transaction));
    } else {
      pulsarMessage.send(producer.newMessage());
    }
  }

  private void sendMessage(
      Destination defaultDestination, Message message, CompletionListener completionListener)
      throws JMSException {
    Producer<byte[]> producer =
        session.getFactory().getProducerForDestination((PulsarDestination) defaultDestination);
    PulsarMessage pulsarMessage = (PulsarMessage) message;
    if (session.transaction != null) {
      pulsarMessage.sendAsync(producer.newMessage(session.transaction), completionListener);
    } else {
      pulsarMessage.sendAsync(producer.newMessage(), completionListener);
    }
  }
}