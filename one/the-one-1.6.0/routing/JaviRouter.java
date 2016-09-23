/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import routing.javi.JaviDijkstra;
import routing.javi.MeetingProbabilitySet;
import routing.javi.NodePositionsSet;
import routing.javi.NodeDistancesSet;
import routing.util.RoutingInfo;
import routing.javi.GlobalMap;

import util.Tuple;
import core.Connection;
import core.SimClock;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.Coord;

/**
 * Implementation of Javi router as described in 
 * <I>Javi: Routing for Vehicle-Based Disruption-Tolerant Networks</I> by
 * John Burgess et al.
 * @version 1.0
 * 
 * Extension of the protocol by adding a parameter alpha (default 1)
 * By new connection, the delivery likelihood is increased by alpha
 * and divided by 1+alpha.  Using the default results in the original 
 * algorithm.  Refer to Karvo and Ott, <I>Time Scales and Delay-Tolerant Routing 
 * Protocols</I> Chants, 2008 
 * 
 * + GPS-less map generator by J. Born
 */



public class JaviRouter extends ActiveRouter {
	
	public boolean firstConnection = true;
	public static final double GLOBAL_TIMEOUT = 100.0;
	public static final int[] FIXED_NODES = {1,10,30};
			
    /** Router's setting namespace ({@value})*/
	public static final String Javi_NS = "JaviRouter";
	/**
	 * Meeting probability set maximum size -setting id ({@value}).
	 * The maximum amount of meeting probabilities to store.  */
	public static final String PROB_SET_MAX_SIZE_S = "probSetMaxSize";
	public static final String NROFSTATIC = "nrofStaticNodes";
	public static final String STATICNODE = "staticNode";

	
    /** Default value for the meeting probability set maximum size ({@value}).*/
    public static final int DEFAULT_PROB_SET_MAX_SIZE = 100;
    private static int probSetMaxSize;
    private int count = -1;
    
    /** how many new nodes should i see to recalculate my local map **/
    private int REC_PERIOD = 3;
    
    /** how many new nodes should i see to recalculate my global map**/
    private int GL_REC_PERIOD = 8;
    
	/** probabilities of meeting hosts */
	private MeetingProbabilitySet probs;
	private NodeDistancesSet dists;
	
	/** local map, and count of static nodes in it**/
	private NodePositionsSet localmap;
	public int staticCount;
	public HashMap<Integer, Coord> staticNodes;
	public HashMap<Integer, DTNHost> seenHosts;
	
	public double mapError;
	public double lastClock;
	private HashMap<Double, Double> avgMapError;
	private HashMap<Double, Integer> mapSize;
	
	/** global map **/
	public GlobalMap globalmap;
	
	/**time to generate a new map**/
	private double timeNewMap;
	
	/** meeting probabilities and distances of all hosts from this host's point
	 * of view mapped using host's network address */
	private Map<Integer, MeetingProbabilitySet> allProbs;
	private Map<Integer, NodeDistancesSet> allDists;
		
	/** the cost-to-node calculator */
	private JaviDijkstra dijkstra;	
	
	/** IDs of the messages that are known to have reached the final dst */
	private Set<String> ackedMessageIds;
	
	/** mapping of the current costs for all messages. This should be set to
	 * null always when the costs should be updated (a host is met or a new
	 * message is received) */
	
	/* TODO: costs usa globalmap como dato también */
	private Map<Integer, Double> costsForMessages;
	/** From host of the last cost calculation */
	private DTNHost lastCostFrom;
	
	/** Map of which messages have been sent to which hosts from this host */
	private Map<DTNHost, Set<String>> sentMessages;
		
	/** Over how many samples the "average number of bytes transferred per
	 * transfer opportunity" is taken */
	public static int BYTES_TRANSFERRED_AVG_SAMPLES = 10;
	private int[] avgSamples;
	private int nextSampleIndex = 0;
	
	/** current value for the "avg number of bytes transferred per transfer
	 * opportunity"  */
	private int avgTransferredBytes = 0;

	/** The alpha parameter string*/
	public static final String ALPHA_S = "alpha";

	/** The alpha variable, default = 1;*/
	private double alpha;

	/** The default value for alpha */
	public static final double DEFAULT_ALPHA = 1.0;

	/**
	 *
	 * Constructor. Creates a new prototype router based on the settings in
	 * the given Settings object.
	 * @param settings The settings object
	 */
	
	
	public JaviRouter(Settings settings) {
		super(settings);
		
		Settings JaviSettings = new Settings(Javi_NS);	
		//alpha
		if (JaviSettings.contains(ALPHA_S)) {
			alpha = JaviSettings.getDouble(ALPHA_S);
		} else {
			alpha = DEFAULT_ALPHA;
		}
		//probsetmaxsize
        if (JaviSettings.contains(PROB_SET_MAX_SIZE_S)) {
            probSetMaxSize = JaviSettings.getInt(PROB_SET_MAX_SIZE_S);
        } else {
            probSetMaxSize = DEFAULT_PROB_SET_MAX_SIZE;
        }
        //norofstatic
        staticCount = JaviSettings.getInt(NROFSTATIC);
        //for static: x, y
        staticNodes = new HashMap<Integer, Coord>();
        for(int i = 0; i < staticCount; i++){
        	String s = JaviSettings.getSetting(STATICNODE+i);
        	String[] n = s.split(" ");
        	try{
        		staticNodes.put(Integer.parseInt(n[0]), new Coord(Double.parseDouble(n[1]),Double.parseDouble(n[2])));
        	}
        	catch(Exception e){
        		System.out.println("Can't parse the static node " + i + " info.");
        		System.exit(1);
        	}
        }
        
	}
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected JaviRouter(JaviRouter r) {
		super(r);

		this.alpha = r.alpha;	
		this.staticNodes = r.staticNodes;
		this.staticCount = r.staticCount;
		this.probs = new MeetingProbabilitySet(probSetMaxSize, this.alpha);
		this.dists = new NodeDistancesSet(probSetMaxSize);
		this.localmap = new NodePositionsSet();
		this.globalmap = new GlobalMap();

		this.seenHosts = new HashMap<Integer, DTNHost>();
		this.mapError = 0.0;
		this.lastClock = 0.0;
		this.avgMapError = new HashMap<Double, Double>();
		this.mapSize = new HashMap<Double, Integer>();
		
		this.allProbs = new HashMap<Integer, MeetingProbabilitySet>();
		this.allDists = new HashMap<Integer, NodeDistancesSet>();
		
		this.dijkstra = new JaviDijkstra(this.allProbs);
		this.ackedMessageIds = new HashSet<String>();
		this.avgSamples = new int[BYTES_TRANSFERRED_AVG_SAMPLES];
		this.sentMessages = new HashMap<DTNHost, Set<String>>();
		

	}	

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		if (con.isUp()) { 

			// new connection

			/** MaxProp**/
			this.costsForMessages = null; // invalidate old cost estimates
			DTNHost otherHost = con.getOtherNode(getHost());
			MessageRouter mRouter = otherHost.getRouter();
			assert mRouter instanceof JaviRouter : "Javi only works with other routers of same type";
			JaviRouter otherRouter = (JaviRouter)mRouter;
			
			/** JaviRouter map**/
			
			
			/*on first connection we set our data*/
			int myID = getHost().getAddress();
			int otherID = otherHost.getAddress();
			
			this.seenHosts.put(otherID, otherHost);
			this.seenHosts.putAll(otherRouter.seenHosts);
			
			if(firstConnection){
				ArrayList<Integer> staticIDs = new ArrayList<Integer>();
				staticIDs.addAll(staticNodes.keySet());
				localmap.setID(myID);
				localmap.setStaticIDs(staticIDs);
				globalmap.setID(myID);
				globalmap.setStaticNodes(staticNodes);
				firstConnection = false;
			}

			//i restart my global map if it is too old.
			//core.Debug.p("globalmap CT: " + (SimClock.getTime() - globalmap.getCT()) + ", GTO:" + GLOBAL_TIMEOUT + ", %0?" + ((SimClock.getTime() - globalmap.getCT())% GLOBAL_TIMEOUT));
			//if((SimClock.getTime() - globalmap.getCT()) % GLOBAL_TIMEOUT == 0.0) globalmap.restart();
			this.localmap.setID(myID);
			this.globalmap.setID(myID);
			/*update my distance to the node*/
			double mapTimeout = dists.update(otherID, getHost().getDistance(otherHost));
			allDists.put(myID, dists);
			allDists.put(otherID, otherRouter.dists);
			/*update distances and calculate new local map*/
			staticCount = localmap.update(allDists, true);
			globalmap.updateMap(localmap);
			
			//if(staticCount > 0)	core.Debug.p("t = " + SimClock.getTime() + ", map computed node" + myID + ", static nodes seen: " + staticCount);
			
			if (con.isInitiator(getHost())) {

				/* exchange ACKed message data */
				this.ackedMessageIds.addAll(otherRouter.ackedMessageIds);
				otherRouter.ackedMessageIds.addAll(this.ackedMessageIds);
				deleteAckedMessages();
				otherRouter.deleteAckedMessages();
				
				/* update both meeting probabilities */
				probs.updateMeetingProbFor(otherHost.getAddress());
				otherRouter.probs.updateMeetingProbFor(getHost().getAddress());

				/* exchange the transitive probabilities */
				this.updateTransitiveProbs(otherRouter.allProbs);
				otherRouter.updateTransitiveProbs(this.allProbs);
				this.allProbs.put(otherHost.getAddress(), otherRouter.probs.replicate());
				otherRouter.allProbs.put(getHost().getAddress(), this.probs.replicate());
				
				/** JaviRouter map **/
				
				/*Exchange to generate global maps. We need at least 3 static nodes to generate a globalmap*/

				//*Case 1: I have enough static nodes, I compute the globalmap and try to add his local*
				if(this.staticCount >= 3){ 
					//core.Debug.p("case1 - my staticcount>3");
					
					//core.Debug.p(localmap.toString());

					/*I get his local map*/
					this.globalmap.addMap(otherRouter.localmap, otherID);
					/*I try to compute globalmap*/
					int glb = this.globalmap.makeGlobal();
					//core.Debug.p("n " + myID + " glb " + glb);
					//core.Debug.p("diff " + myID + ": " + this.globalmap.getGlobalCoord(myID).toString() + " vs " + myLoc.toString());
					//core.Debug.p("1-glb: " + this.globalmap.toString());
					/* If I succeed, send globalmap and update his with mine */
					if(glb > 0){
						//mapError();
						otherRouter.globalmap.addMap(this.globalmap);
					}/* If I fail, send localmap */
					else{
						otherRouter.globalmap.addMap(this.localmap, myID);
					}
					//core.Debug.p("t = " + SimClock.getTime() + " node:" + getHost().getAddress() + " " + globalmap.toString() );

				}
				/* Case 2: The other node has enough static nodes,
				 * he computes the globalmap and shares with me
				 * */
				else if(otherRouter.staticCount>= 3){
					//core.Debug.p("case2 - otherstaticcount>3");
					/*I send my local map*/
					otherRouter.globalmap.addMap(this.localmap, myID);
					/*Other triescomputes globalmap*/
					int glb = otherRouter.globalmap.makeGlobal();
					//core.Debug.p("n" + otherID);
					//core.Debug.p("diff " + otherID + ": " + otherRouter.globalmap.getGlobalCoord(otherID).toString() + " vs " + otherLoc.toString());
					//core.Debug.p("2-glb: " + otherRouter.globalmap.toString());
					/* If he succeeds, send me his globalmap and update mine with his */
					if(glb > 0){
						this.globalmap.addMap(otherRouter.globalmap);
						//mapError();
					}/* If he fails, he just sends me his localmap */
					else{
						this.globalmap.addMap(otherRouter.localmap, otherID);
					}
					//core.Debug.p("t = " + SimClock.getTime() + "node:" + getHost().getAddress() + " " + globalmap.toString() );

				}
				
				/*Case 3: We don't have static nodes
				 * we just add our localmaps for future update
				 */
				else if(this.staticCount < 3 || otherRouter.staticCount < 3){
					//core.Debug.p("case3 - no static nodes");
					this.globalmap.addMap(otherRouter.localmap, otherID);
					otherRouter.globalmap.addMap(this.localmap, myID);
				}
				else{
					//core.Debug.p("case4 - other case: mysc" + this.staticCount + ", osc:" + otherRouter.staticCount);
				}
				
			}
		}
		else {
			/* connection went down, update transferred bytes average */
			updateTransferredBytesAvg(con.getTotalBytesTransferred());
		}
	}

	/**
	 * Computes the error of the current global map
	 * 
	 */
	public void mapError(){
		Map<Integer, Coord> global = this.globalmap.getGlobalMap();
		if(global != null && (SimClock.getTime() > lastClock + 1.0)){
			core.Debug.p("**maperror**");
			this.mapError = 0.0;
			double currentError = 0.0;
			int maxErrorNode = 0;
			double maxError = 0.0;
			int ct = 0;
			if(global.size() > 0){
				for(Map.Entry<Integer, Coord> pos : global.entrySet()){
					if(this.seenHosts.containsKey(pos.getKey())){
						currentError = pos.getValue().distance(this.seenHosts.get(pos.getKey()).getLocation());
						if(maxError < currentError){ 
							maxError = currentError;
							maxErrorNode = pos.getKey();
						}
						mapError+=currentError;
					}
					else ct++;
				}
				this.mapError = mapError/(global.size()-ct);
				core.Debug.p("*** node: " + this.globalmap.myID);
				core.Debug.p("time: " + SimClock.getTime());
				core.Debug.p("avg error: " + mapError);
				core.Debug.p("max error: " + maxError + " on node:" + maxErrorNode);
				core.Debug.p("size: " + global.size());
			}
			this.lastClock = SimClock.getTime();
			this.avgMapError.put(lastClock, this.mapError);
			this.mapSize.put(lastClock, global.size());
		}
	}
	/**
	 * Updates transitive probability values by replacing the current 
	 * MeetingProbabilitySets with the values from the given mapping
	 * if the given sets have more recent updates.
	 * @param p Mapping of the values of the other host
	 */
	private void updateTransitiveProbs(Map<Integer, MeetingProbabilitySet> p) {
		for (Map.Entry<Integer, MeetingProbabilitySet> e : p.entrySet()) {
			MeetingProbabilitySet myMps = this.allProbs.get(e.getKey()); 
			if (myMps == null || 
				e.getValue().getLastUpdateTime() > myMps.getLastUpdateTime() ) {
				this.allProbs.put(e.getKey(), e.getValue().replicate());
			}
		}
	}
	
	/**
	 * Deletes the messages from the message buffer that are known to be ACKed
	 */
	private void deleteAckedMessages() {
		for (String id : this.ackedMessageIds) {
			if (this.hasMessage(id) && !isSending(id)) {
				this.deleteMessage(id, false);
			}
		}
	}
	
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		this.costsForMessages = null; // new message -> invalidate costs
		Message m = super.messageTransferred(id, from);
		/* was this node the final recipient of the message? */
		if (isDeliveredMessage(m)) {
			this.ackedMessageIds.add(id);
		}
		return m;
	}
	
	/**
	 * Method is called just before a transfer is finalized 
	 * at {@link ActiveRouter#update()}. Javi makes book keeping of the
	 * delivered messages so their IDs are stored.
	 * @param con The connection whose transfer was finalized
	 */
	@Override
	protected void transferDone(Connection con) {
		Message m = con.getMessage();
		String id = m.getId();
		DTNHost recipient = con.getOtherNode(getHost());
		Set<String> sentMsgIds = this.sentMessages.get(recipient);
		
		/* was the message delivered to the final recipient? */
		if (m.getTo() == recipient) { 
			this.ackedMessageIds.add(m.getId()); // yes, add to ACKed messages
			this.deleteMessage(m.getId(), false); // delete from buffer
		}
		
		/* update the map of where each message is already sent */
		if (sentMsgIds == null) {
			sentMsgIds = new HashSet<String>();
			this.sentMessages.put(recipient, sentMsgIds);
		}		
		sentMsgIds.add(id);
	}
	
	/**
	 * Updates the average estimate of the number of bytes transferred per
	 * transfer opportunity.
	 * @param newValue The new value to add to the estimate
	 */
	private void updateTransferredBytesAvg(int newValue) {
		int realCount = 0;
		int sum = 0;
		
		this.avgSamples[this.nextSampleIndex++] = newValue;
		if(this.nextSampleIndex >= BYTES_TRANSFERRED_AVG_SAMPLES) {
			this.nextSampleIndex = 0;
		}
		
		for (int i=0; i < BYTES_TRANSFERRED_AVG_SAMPLES; i++) {
			if (this.avgSamples[i] > 0) { // only values above zero count
				realCount++;
				sum += this.avgSamples[i];
			}
		}
		
		if (realCount > 0) {
			this.avgTransferredBytes = sum / realCount;
		}
		else { // no samples or all samples are zero
			this.avgTransferredBytes = 0;
		}
	}
	
	/**
	 * Returns the next message that should be dropped, according to Javi's
	 * message ordering scheme (see {@link JaviTupleComparator}). 
	 * @param excludeMsgBeingSent If true, excludes message(s) that are
	 * being sent from the next-to-be-dropped check (i.e., if next message to
	 * drop is being sent, the following message is returned)
	 * @return The oldest message or null if no message could be returned
	 * (no messages in buffer or all messages in buffer are being sent and
	 * exludeMsgBeingSent is true)
	 */
    @Override
	protected Message getNextMessageToRemove(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		List<Message> validMessages = new ArrayList<Message>();

		for (Message m : messages) {	
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			validMessages.add(m);
		}
		
		Collections.sort(validMessages, 
				new JaviComparator(this.calcThreshold())); 
		
		return validMessages.get(validMessages.size()-1); // return last message
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		tryOtherMessages();	
	}
	
	/**
	 * Returns the message delivery cost between two hosts from this host's
	 * point of view. If there is no path between "from" and "to" host, 
	 * Double.MAX_VALUE is returned. Paths are calculated only to hosts
	 * that this host has messages to.
	 * @param from The host where a message is coming from
	 * @param to The host where a message would be destined to
	 * @return The cost of the cheapest path to the destination or 
	 * Double.MAX_VALUE if such a path doesn't exist
	 */
	public double getCost(DTNHost from, DTNHost to) {
		/* check if the cached values are OK */
		if (this.costsForMessages == null || lastCostFrom != from) {
			/* cached costs are invalid -> calculate new costs */
			this.allProbs.put(getHost().getAddress(), this.probs);
			int fromIndex = from.getAddress();
			
			/* calculate paths only to nodes we have messages to 
			 * (optimization) */
			Set<Integer> toSet = new HashSet<Integer>();
			for (Message m : getMessageCollection()) {
				toSet.add(m.getTo().getAddress());
			}
						
			this.costsForMessages = dijkstra.getCosts(fromIndex, toSet);
			this.lastCostFrom = from; // store source host for caching checks
		}
		
		if (costsForMessages.containsKey(to.getAddress())) {
			return costsForMessages.get(to.getAddress());
		}
		else {
			/* there's no known path to the given host */
			return Double.MAX_VALUE;
		}
	}
	
	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * hop counts and their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		Collection<Message> msgCollection = getMessageCollection();
		
		/* for all connected hosts that are not transferring at the moment,
		 * collect all the messages that could be sent */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			JaviRouter othRouter = (JaviRouter)other.getRouter();
			Set<String> sentMsgIds = this.sentMessages.get(other);
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			for (Message m : msgCollection) {
				/* skip messages that the other host has or that have
				 * passed the other host */
				if (othRouter.hasMessage(m.getId()) ||
						m.getHops().contains(other)) {
					continue; 
				}
				/* skip message if this host has already sent it to the other
				   host (regardless of if the other host still has it) */
				if (sentMsgIds != null && sentMsgIds.contains(m.getId())) {
					continue;
				}
				/* message was a good candidate for sending */
				messages.add(new Tuple<Message, Connection>(m,con));
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		/* sort the message-connection tuples according to the criteria
		 * defined in JaviTupleComparator */ 
		Collections.sort(messages, new JaviTupleComparator(calcThreshold()));
		return tryMessagesForConnected(messages);	
	}
	
	/**
	 * Calculates and returns the current threshold value for the buffer's split
	 * based on the average number of bytes transferred per transfer opportunity
	 * and the hop counts of the messages in the buffer. Method is public only
	 * to make testing easier.  
	 * @return current threshold value (hop count) for the buffer's split
	 */
	public int calcThreshold() {
		/* b, x and p refer to respective variables in the paper's equations */
		int b = this.getBufferSize();
		int x = this.avgTransferredBytes;
		int p;

		if (x == 0) {
			/* can't calc the threshold because there's no transfer data */
			return 0;
		}
		
		/* calculates the portion (bytes) of the buffer selected for priority */
		if (x < b/2) {
			p = x;
		}
		else if (b/2 <= x && x < b) {
			p = Math.min(x, b-x);
		}
		else {
			return 0; // no need for the threshold 
		}
		
		/* creates a copy of the messages list, sorted by hop count */
		ArrayList<Message> msgs = new ArrayList<Message>();
		msgs.addAll(getMessageCollection());
		if (msgs.size() == 0) {
			return 0; // no messages -> no need for threshold
		}
		/* anonymous comparator class for hop count comparison */
		Comparator<Message> hopCountComparator = new Comparator<Message>() {
			public int compare(Message m1, Message m2) {
				return m1.getHopCount() - m2.getHopCount();
			}
		};
		Collections.sort(msgs, hopCountComparator);

		/* finds the first message that is beyond the calculated portion */
		int i=0;
		for (int n=msgs.size(); i<n && p>0; i++) {
			p -= msgs.get(i).getSize();
		}
		
		i--; // the last round moved i one index too far 
		if (i < 0) {
			return 0;
		}
		
		/* now i points to the first packet that exceeds portion p; 
		 * the threshold is that packet's hop count + 1 (so that packet and
		 * perhaps some more are included in the priority part) */
		return msgs.get(i).getHopCount() + 1;
	}
	
	/**
	 * Message comparator for the Javi routing module. 
	 * Messages that have a hop count smaller than the given
	 * threshold are given priority and they are ordered by their hop count.
	 * Other messages are ordered by their delivery cost.
	 */
	private class JaviComparator implements Comparator<Message> {
		private int threshold;
		private DTNHost from1;
		private DTNHost from2;
		
		/**
		 * Constructor. Assumes that the host where all the costs are calculated
		 * from is this router's host.
		 * @param treshold Messages with the hop count smaller than this
		 * value are transferred first (and ordered by the hop count)
		 */
		public JaviComparator(int treshold) {
			this.threshold = treshold;
			this.from1 = this.from2 = getHost();
		}

		/**
		 * Constructor. 
		 * @param treshold Messages with the hop count smaller than this
		 * value are transferred first (and ordered by the hop count)
		 * @param from1 The host where the cost of msg1 is calculated from
		 * @param from2 The host where the cost of msg2 is calculated from 
		 */
		public JaviComparator(int treshold, DTNHost from1, DTNHost from2) {
			this.threshold = treshold;
			this.from1 = from1;
			this.from2 = from2;
		}

		/**
		 * Compares two messages and returns -1 if the first given message
		 * should be first in order, 1 if the second message should be first
		 * or 0 if message order can't be decided. If both messages' hop count
		 * is less than the threshold, messages are compared by their hop count 
		 * (smaller is first). If only other's hop count is below the threshold,
		 * that comes first. If both messages are below the threshold, the one
		 * with smaller cost (determined by 
		 * {@link JaviRouter#getCost(DTNHost, DTNHost)}) is first. 
		 */
		public int compare(Message msg1, Message msg2) {
			double p1, p2;
			int hopc1 = msg1.getHopCount();
			int hopc2 = msg2.getHopCount();

			if (msg1 == msg2) {
				return 0;
			}
			
			/* if one message's hop count is above and the other one's below the 
			 * threshold, the one below should be sent first */
			if (hopc1 < threshold && hopc2 >= threshold) {
				return -1; // message1 should be first
			}
			else if (hopc2 < threshold && hopc1 >= threshold) {
				return 1; // message2 -"-
			}
			
			/* if both are below the threshold, one with lower hop count should
			 * be sent first */
			if (hopc1 < threshold && hopc2 < threshold) {
				return hopc1 - hopc2;
			}
			
			/* both messages have more than threshold hops -> cost of the
			 * message path is used for ordering */
			p1 = getCost(from1, msg1.getTo());
			p2 = getCost(from2, msg2.getTo());
			
			/* the one with lower cost should be sent first */
			if (p1-p2 == 0) {
				/* if costs are equal, hop count breaks ties. If even hop counts
				   are equal, the queue ordering is used  */
				if (hopc1 == hopc2) {
					return compareByQueueMode(msg1, msg2);
				}
				else {
					return hopc1 - hopc2;	
				}
			}
			else if (p1-p2 < 0) {
				return -1; // msg1 had the smaller cost
			}
			else {
				return 1; // msg2 had the smaller cost
			}
		}		
	}
	
	/**
	 * Message-Connection tuple comparator for the Javi routing 
	 * module. Uses {@link JaviComparator} on the messages of the tuples
	 * setting the "from" host for that message to be the one in the connection
	 * tuple (i.e., path is calculated starting from the host on the other end 
	 * of the connection).
	 */
	private class JaviTupleComparator 
			implements Comparator <Tuple<Message, Connection>>  {
		private int threshold;
		
		public JaviTupleComparator(int threshold) {
			this.threshold = threshold;
		}
		
		/**
		 * Compares two message-connection tuples using the 
		 * {@link JaviComparator#compare(Message, Message)}.
		 */
		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			JaviComparator comp;
			DTNHost from1 = tuple1.getValue().getOtherNode(getHost());
			DTNHost from2 = tuple2.getValue().getOtherNode(getHost());
			
			comp = new JaviComparator(threshold, from1, from2);
			return comp.compare(tuple1.getKey(), tuple2.getKey());
		}
	}
	
	
	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(probs.getAllProbs().size() + " meeting probabilities");
		
		/* show meeting probabilities for this host */
		for (Map.Entry<Integer, Double> e : probs.getAllProbs().entrySet()) {
			Integer host = e.getKey();
			Double value = e.getValue();			
			ri.addMoreInfo(new RoutingInfo(String.format("host %d : %.6f", 
					host, value)));
		}
			
		top.addMoreInfo(ri);
		top.addMoreInfo(new RoutingInfo("Avg transferred bytes: " + 
				this.avgTransferredBytes));

		return top;
	}
	@Override
	protected void transferAborted(Connection con) {
		/*just before connection finishes, i update distances again*/
		DTNHost otherHost = con.getOtherNode(getHost());
		this.dists.update(otherHost.getAddress(), getHost().getDistance(otherHost));

	}

	
	@Override
	public MessageRouter replicate() {
		JaviRouter r = new JaviRouter(this);
		return r;
	}
}