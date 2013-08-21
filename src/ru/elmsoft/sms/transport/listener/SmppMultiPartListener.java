/**
 * 
 */
package ru.elmsoft.sms.transport.listener;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.objectxp.msg.MessageEvent;
import com.objectxp.msg.MessageEventListener;
import com.objectxp.msg.SmppMessage;
import com.objectxp.msg.SmppOptionalParameter;

/**
 * Этот класс выступает как proxy для других listener. Он пропускает через себя
 * все события, кроме MESSAGE_RECEIVE. При получении сообщения сначала
 * анализируется, что сообщение не является частью большого сообщения. Если нет -
 * то пропускаем дальше, иначе ожидаются остальные сообщения, текст сообщения
 * склеивается и Listeners получает одно, но большое сообщение.
 * 
 */
public class SmppMultiPartListener implements MessageEventListener {
	/**
	 * список частей сообщений
	 */
	private List poolMessages = Collections.synchronizedList(new LinkedList());
	/**
	 * Список слушателей
	 */
	private List listeners = Collections.synchronizedList(new LinkedList());

	static class PoolEntity {
		SmppMessage message;
		int msg_ref_num;
		int msg_seqnum;
		int total_segments;
	}

	public void handleMessageEvent(MessageEvent evt) {
		if (evt.getType() != MessageEvent.MESSAGE_RECEIVED) {
			Logger.getLogger(SmppMultiPartListener.class).debug("Event is not MESSAGE_RECEIVED... Translate this to other listeners");
			dispatchEvents(evt);
			return;
		}
		Logger.getLogger(SmppMultiPartListener.class).debug("Event is MESSAGE_RECEIVED... Analyze TLV");
		if (!(evt.getMessage() instanceof SmppMessage)) {
			Logger.getLogger(getClass()).warn("Message is not smpp! Dispath this event without any analyze");
			dispatchEvents(evt);
			return;
		}
		SmppMessage msg = (SmppMessage) evt.getMessage();
		if (msg.getOptionalParameter(SmppOptionalParameter.TAG_SAR_MSG_REF_NUM) == null) {
			Logger.getLogger(SmppMultiPartListener.class).debug("Message has not sar-tags. Translate this other listeners");
			dispatchEvents(evt);
			return;
		}
		Logger.getLogger(getClass()).debug("Message has  sar-tags, we want process this message....");
		PoolEntity part_msg = new PoolEntity();

		Logger.getLogger(getClass()).debug("Analyze TLV SAR_MSG_REF_NUM " + msg.getOptionalParameter(SmppOptionalParameter.TAG_SAR_MSG_REF_NUM));
		part_msg.msg_ref_num = convertByteArrayToInt(msg.getOptionalParameter(SmppOptionalParameter.TAG_SAR_MSG_REF_NUM).getValue());

		Logger.getLogger(getClass()).debug("Analyze TLV TAG_SAR_SEGMENT_SEQNUM " + msg.getOptionalParameter(SmppOptionalParameter.TAG_SAR_SEGMENT_SEQNUM));
		part_msg.msg_seqnum = convertByteArrayToInt(msg.getOptionalParameter(SmppOptionalParameter.TAG_SAR_SEGMENT_SEQNUM).getValue());

		Logger.getLogger(getClass()).debug("Analyze TLV TAG_SAR_SEGMENT_SEQNUM " + msg.getOptionalParameter(SmppOptionalParameter.TAG_SAR_TOTAL_SEGMENTS));
		part_msg.total_segments = convertByteArrayToInt(msg.getOptionalParameter(SmppOptionalParameter.TAG_SAR_TOTAL_SEGMENTS).getValue());

		part_msg.message = msg;
		poolMessages.add(part_msg);

		Logger.getLogger(getClass()).debug("processing pool message");
		PoolEntity[] msg_array = new PoolEntity[part_msg.total_segments];
		for (Iterator iterator = poolMessages.iterator(); iterator.hasNext();) {
			PoolEntity obj = (PoolEntity) iterator.next();
			if (obj.msg_ref_num == part_msg.msg_ref_num)
				msg_array[obj.msg_seqnum - 1] = obj;
		}
		String split_txt = "";
		for (int i = 0; i < msg_array.length; i++) {
			if (msg_array[i] == null) {
				Logger.getLogger(getClass()).debug("We have not all part's message and wa need to wait this");
				return;
			} else {
				split_txt = split_txt.concat(msg_array[i].message.getMessage());
			}
		}
		Logger.getLogger(getClass()).debug("Removing parts of message from pool");
		for (Iterator iterator = poolMessages.iterator(); iterator.hasNext();) {
			PoolEntity obj = (PoolEntity) iterator.next();
			if (obj.msg_ref_num == part_msg.msg_ref_num)
				iterator.remove();
		}
		Logger.getLogger(getClass()).debug("Change text in last message and dispatch event to listener");
		evt.getMessage().setMessage(split_txt);
		dispatchEvents(evt);

	}

	private int convertByteArrayToInt(byte[] buffer) {
		int multiply = 1;
		int result = 0;
		for (int i = buffer.length - 1; i >= 0; i--) {
			byte b = buffer[i];
			result += b * multiply;
			multiply *= 256;
		}
		Logger.getLogger(getClass()).debug("convertByteArrayToInt result is " + result);
		return result;
	}

	private void dispatchEvents(MessageEvent evt) {
		for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
			MessageEventListener obj = (MessageEventListener) iterator.next();
			obj.handleMessageEvent(evt);
		}
	}

	public void addListener(MessageEventListener listener) {
		listeners.add(listener);
	}

}
