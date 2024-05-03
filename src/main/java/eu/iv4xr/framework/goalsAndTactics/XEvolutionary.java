package eu.iv4xr.framework.goalsAndTactics;

import static nl.uu.cs.aplib.AplibEDSL.SEQ;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import eu.iv4xr.framework.goalsAndTactics.BasicSearch.AlgorithmResult;
import nl.uu.cs.aplib.utils.Pair;

/**
 * Implementing an evolutionary-search algorithm.
 * 
 * <p>
 * <b>Chromosome</b>: is a sequence of interactables. So, the genes are game's
 * interactables (or at least their ids).
 * 
 * <p>
 * <b>Fitness</b> of a chromosome: the entities in the chromosome are
 * interacted, in the sequence they appear. This is done by aplib agent. If this
 * execution manages to achieve the top-goal, the fitness will be some
 * max-value specified in {@link BasicSearch#maxReward}. Else the fitness value
 * of the chromosome is the value of the final state at the end of the chromosome
 * execution. This is specified in {@link BasicSearch#stateValueFunction}.
 * 
 * <p>
 * The algorithm does not know what are available interactables. So it starts by
 * exploring the target game to collect an initial set of known interactables.
 * Later, whenever a chromosome is executed, exploration is added after every
 * execution of a gene. Newly discovered interactables are added to the set of
 * known interactables.
 * 
 * <p>
 * The set of initial chromosomes will be singleton chromosomes, each containing
 * one button from the set of known buttons obtained from the initial
 * exploration when the algoroithm starts.
 * 
 * <p>
 * The algorithm proceeds as follows:
 * 
 * <ul>
 * <li>(1) explore then create the initial population P
 * <li>(2) WHILE termination-condition is still false:
 * <ul>
 * <li>(3) select a set of parents from P. The used selection scheme is
 * currently set to select some K chromosomes with best fitness, and then to
 * fill it with random selection from the rest of P. The number of selected
 * parents is limited to some number (we use {{@link #maxPopulationSize}/2).
 * <li>(4) Randomly choose two parents. Decide whether to keep them, or to do
 * cross-over. Cross-over of two parents (p1,p2) creates two new chromosomes
 * based on the parents. They will replace the parents. After doing this we have
 * a set of new chromosomes; let's call it Q.
 * <li>(5) Generates new chromosomes from Q by either mutating them or extending
 * them. Mutating a chromosome ch means we replace one interactable in it with
 * another (from the set of known interactables!). Extending ch means to
 * randomly insert a new interactable somewhere in ch. We fill Q with these new
 * chromosomes, up to some maximim size ({@link #maxPopulationSize}).
 * <li>(6) We replace P with Q.
 * <li>(7) For each chromosome in P we calculate its fitness-value.
 * 
 * </ul>
 * </ul>
 */
public class XEvolutionary extends BasicSearch {

	public float mutationProbability  = 0.2f ;
	public float insertionProbability = 0.3f ;
	public float crossoverProbability = 0.2f ;
	
	/**
	 * When true, then the extend-operation inserts a gene that is not already in
	 * the target chromosome. Default is true.
	 */
	public boolean onlyExtendWithNewGene = true ;
	
	/**
	 * When false, then the extend-operation inserts a new gene at a random position, and else
	 * it is added at the end. Default is true.
	 */
	public boolean extendAtRandomInsertionPoint = true ;
	
	/**
	 * Should be at least four. Default: 20.
	 */
	public int maxPopulationSize = 20 ;
	
	/**
	 * Default 10.
	 */
	public int numberOfElitesToKeepDuringSelection = 10 ;
		
	public int generationNr = 0 ;
	
	/**
	 * If defined, this puts an uppperbound on the maximum number of generations
	 * of populations to evolve. Default: null.
	 */
	public Integer maxNumberOfGenerations = null ;
		
	List<String> knownInteractables = new LinkedList<>() ;
	
	public static class ChromosomeInfo {
		public List<String> chromosome ;
		public float fitness ;
		//public XBelief belief ;
		
		ChromosomeInfo(List<String> chromosome, float value) {
			this.chromosome = chromosome ;
			this.fitness = value ;
		}
	}
	
	public static class Population {
		
		Random rnd  ;
				
		List<ChromosomeInfo> population = new LinkedList<>() ;
		
		/**
		 * Add a new chromosome, and keep the population sorted by the chromosomes' values.
		 */
		void add(ChromosomeInfo CI) {
 			if (population.isEmpty()) {
 				population.add(CI) ;
 				return ;
 			}
 			if (population.get(population.size() - 1).fitness >= CI.fitness) {
 				population.add(CI) ;
 				return ;
 			}
			int k = 0 ;
			for (var M : population) {
				if (M.fitness < CI.fitness) {
					break ;
				}
				k++ ;
			}
			population.add(k,CI) ;		
		}
		
		
		public ChromosomeInfo getBest() {
			if (population.isEmpty()) return null ;
			return population.get(0) ;
		}
		
		/**
		 * Get the Chromosome-info of the given chromosome, if it is in the population. Else
		 * return null.
		 */
		public ChromosomeInfo getInfo(List<String> chromosome) {
			for (var CI : population) {
				if (CI.chromosome.equals(chromosome)) {
					return CI ;
				}
			}
			return null ;
		}
		
		boolean memberOf(List<String> tau) {
			return population.stream().anyMatch(CI -> tau.equals(CI.chromosome)) ;
		}
		
		void remove(List<String> tau) {
			int k = 0 ;
			for (var CI : population) {
				if (CI.chromosome.equals(tau)) {
					break ;
				}
				k++ ;
			}
			if (k < population.size())
				population.remove(k) ;
		}
		
		/**
		 * Shrink the population to the given target size, keeping the specified
		 * number of the best chromosomes (elitism). The remaining space is filled
		 * by randomly selecting from those outside the elite set.
		 */
		void applySelection(int targetSize, int numberOfElitesToKeep) {
			if (numberOfElitesToKeep > targetSize) 
				throw new IllegalArgumentException() ;
	
			int numberToDrop = population.size() - targetSize ;
			
			while (numberToDrop > 0) {
				int k = rnd.nextInt(population.size() - numberOfElitesToKeep) ;
				k += numberOfElitesToKeep ;
				population.remove(k) ;
				numberToDrop -- ;
			}
		}
		
		@Override
		public String toString() {
			int k=0 ;
			String z = "** #chromosomes=" + population.size() ;
			for (var CI : population) {
				z += "\n** [" + k + "] val=" + CI.fitness + ", " + CI.chromosome ;
				k++ ;
			}
			return z ;
		}				
	}	
	
	public Population myPopulation = new Population() ;
	
	
	public XEvolutionary() { 
		super() ;
		algName = "Evo" ;
		myPopulation.rnd = this.rnd ;
	}
	
	@Override
	public void setRndSeed(int seed) {
		super.setRndSeed(seed);
		myPopulation.rnd = rnd ;
	}
		
	public String showStatus() {
		String z = "** Generation = " + generationNr ;
		z += "\n** #population= " + myPopulation.population.size() ;
		if (myPopulation.population.isEmpty()) 
			return z ;
		z += "\n** best-fitness-value = " + myPopulation.population.get(0).fitness ;
		var avrg = myPopulation.population.stream().collect(Collectors.averagingDouble(CI -> (double) CI.fitness)) ;
		z += "\n** avrg-fitness-value = " + avrg ;
		z += "\n" + myPopulation ;
		return z ;
	}
	
	/**
	 * For creating the initially population of chromosomes. The agent will first explore the game,
	 * to find buttons. Chromosomes of length 1 are then created. Each containing an interaction
	 * with a button.
	 */
	void createInitialPopulation() throws Exception {
		
		if (maxPopulationSize <= 0)
			throw new IllegalArgumentException() ;
		
		if (this.maxDepth <= 0)
			throw new IllegalArgumentException() ;

		knownInteractables.clear();
		initializeEpisode();
		if (exploredG != null) {
			solveGoal("Exploration", exploredG.apply(null), explorationBudget);			
		}
		var interactables = wom().elements.values().stream()
				.filter(e -> isInteractable.test(e))
				.map(e -> e.id)
				.collect(Collectors.toList());
		knownInteractables.addAll(interactables) ;		
		closeEnv_() ;
		
		if (knownInteractables.isEmpty()) {
			throw new Exception("Cannot create a starting population because the agent cannot find any button.") ;
		}
			
		while(interactables.size() > 0 && myPopulation.population.size() < maxPopulationSize) {
			var e = interactables.remove(rnd.nextInt(interactables.size())) ;
			List<String> tau = new LinkedList<>() ; 
			tau.add(e) ;
			myPopulation.add(fitnessValue(tau));
			totNumberOfEpisodes++ ;
			if (stopAfterGoalIsAchieved && goalHasBeenAchieved) break ;	
		}	
		generationNr = 1  ;
	}
	
	/**
	 * Calculate the fitness-value of the chromosome. This is done by converting
	 * the chromosome to a sequence of goals, and have an agent to execute it. 
	 * The execution stops when a gene (as a goal) fails, and the fitness will be
	 * calculated at the state that results from the execution so far.
	 * 
	 */
	ChromosomeInfo fitnessValue(List<String> chromosome) throws Exception {

		initializeEpisode();
		
		log(">>> evaluating chromosome: " + chromosome);
		
		boolean goalPredicateSolved = false ;
		boolean agentIsAlive = true ;
		
		List<String> trace = new LinkedList<>() ;
		
		int k = 0 ;
		for (var e : chromosome) {
			var G = SEQ(reachedG.apply(e), interactedG.apply(e));
			var status = solveGoal("Reached and interacted " + e, G, budget_per_task);
			trace.add(e) ;
			
			if (topGoalPredicate.test(agentState())) {
				markThatGoalIsAchieved(trace) ;
				log("*** Goal is ACHIEVED");
				break ;
			}
			if (agentIsDead()) {
				log("*** The agent is DEAD.");
				break;
			}
			// also break if interacting with e failed:
			if (status.failed())
				break ;
			
			// If explorationG is defined .... explore, then check again
			if (exploredG != null) {
				wipeoutMemory.apply(agent) ;
				solveGoal("Exploration", exploredG.apply(null), explorationBudget);
				if (topGoalPredicate.test(agentState())) {
					markThatGoalIsAchieved(trace) ;
					log("*** Goal is ACHIEVED");
					break ;
				}
				if (agentIsDead()) {
					log("*** The agent is DEAD.");
					break;
				}
			}
			k++ ;
		}
		closeEnv_() ;
			
		// drop the trailing part of the chromosome that were not used (e.g. because the goal is
		// already reached:
		int tobeRemoved = chromosome.size() - k  ;
		while (tobeRemoved > 0) {
			chromosome.remove(chromosome.size()-1) ;
			tobeRemoved -- ;
		}
		
		// also add newly-found interactables to the list of known interactables:
		var interactables = wom().elements.values().stream()
				.filter(e -> isInteractable.test(e))
				.map(e -> e.id)
				.filter(eid -> ! knownInteractables.contains(eid))
				.collect(Collectors.toList());
		knownInteractables.addAll(interactables) ;

		float fitness = clampedValueOfCurrentGameState() ;
		log(">>> chromosome: " 
		   + chromosome
		   + ", FITNESS-VAL=" + fitness);
		return new ChromosomeInfo(chromosome,fitness) ;
	}
	
	
	List<String> copy(List<String> chromosome) {
		var S = new LinkedList<String>() ;
		S.addAll(chromosome) ;
		return S ;
	}
	/**
	 * Return a new chromosome, obtained by randomly mutating one location in 
	 * the given chromosome.
	 * It returns null, if the method fails to mutate.
	 */
	List<String> mutate(List<String> chromosome) {
		
		if (chromosome.size() == 0) return null ;
		
		var S = copy(chromosome) ;
		
		int mutationPoint = rnd.nextInt(S.size()) ;
		String B = S.get(mutationPoint) ;
		List<String> mutations = knownInteractables.stream()
				.filter(A -> ! A.equals(B))
				.collect(Collectors.toList()) ;
		if (mutations.isEmpty()) return null ;
		String M = mutations.get(rnd.nextInt(mutations.size())) ;
		S.set(mutationPoint, M) ;
				
		return S ;
	}
	
	/**
	 * Insert a new gene into a chromosome. The method fails if no gene to insert can be found.
	 */
	List<String> extend(List<String> chromosome) {
		
		if (chromosome.size() == 0) return null ;
		
		var seq = copy(chromosome) ;
				
		
		// insert an interacttion that is not already in the chromosome:
		List<String> candidates = knownInteractables ;	
		if (onlyExtendWithNewGene) 
			candidates = knownInteractables.stream()
				.filter(A -> ! seq.contains(A))
				.collect(Collectors.toList()) ;
		
		if (candidates.isEmpty()) return null ;
		
		String E =  candidates.get(rnd.nextInt(candidates.size())) ;
		
		if (this.extendAtRandomInsertionPoint) {
			int insertionPoint = rnd.nextInt(seq.size()) ;
			seq.add(insertionPoint,E) ;
		}
		else
			seq.add(E) ;
		
		return seq ;	
	}
	
	/**
	 * Create two offsprings of the given chromosomes through cross-over.
	 */
	Pair<List<String>,List<String>> crossOver(List<String> chromosome1, List<String> chromosome2) {
		
		if (chromosome1.size() < 2 || chromosome2.size() < 2)
			return null ;
		
		List<String> shorter = new LinkedList<>() ;
		List<String> longer  = new LinkedList<>() ;
		if (chromosome1.size() >= chromosome2.size()) {
			longer.addAll(chromosome1) ;
			shorter.addAll(chromosome2) ;
		}
		else {
			longer.addAll(chromosome2) ;
			shorter.addAll(chromosome1) ;
		}
		
		int crossPoint = shorter.size()/2 ;
		
		var S1 = new LinkedList<String>() ;
		var S2 = new LinkedList<String>() ;
		
		S1.addAll(shorter.subList(0, crossPoint)) ;
		S1.addAll(longer.subList(crossPoint, longer.size())) ;
		
		S2.addAll(longer.subList(0, crossPoint)) ;
		S2.addAll(shorter.subList(crossPoint, shorter.size())) ;
		
		return new Pair<>(S1,S2) ;
 	}
	

	void evolve() throws Exception {
		
		int halfSize = maxPopulationSize/2 ;
		// Apply selection, drop some chromosones to get the population to maxSize/2.
		// If the current population size is less that maxSize/2, then none is dropped. 
		// The obtained selection is called "parents".
		myPopulation.applySelection(halfSize, numberOfElitesToKeepDuringSelection);
		List<List<String>> parents = new LinkedList<>() ;
		parents.addAll(myPopulation.population.stream().map(CI -> CI.chromosome).collect(Collectors.toList())) ;
		
		// Create a new-batch by either applying crossover or by just putting parents in the
		// new batch.
		List<List<String>> newBatch = new LinkedList<>() ;
		while (parents.size() > 1) {
			var p1 = parents.remove(rnd.nextInt(parents.size()-1)) ;
			List<String> p2 = null ;
			if (parents.size() == 1) {
				p2 = parents.remove(0) ;
			}
			else {
				p2 = parents.remove(rnd.nextInt(parents.size()-1)) ;
			}
		
			boolean putBackParents = true ;
			if (rnd.nextFloat() <= crossoverProbability) {
				var offsprings = crossOver(p1,p2) ;
				if (offsprings != null 
						&& ! newBatch.contains(offsprings.fst)
						&& ! newBatch.contains(offsprings.snd)) {
					newBatch.add(offsprings.fst) ;
					newBatch.add(offsprings.snd) ;
					putBackParents = false ;
				}
			}
			if (putBackParents) {
				newBatch.add(p1) ;
				newBatch.add(p2) ;
 			}	
		}
		if (parents.size() == 1) {
			// a single parent remains, just put it back:
			newBatch.add(parents.remove(0)) ;
		}
		
		// fill in the rest of the new-batch with mutated or extended chromosomes:
		
		int N = newBatch.size() ;
		
		for (int i=0; i<N; i++) {
			var sigma = newBatch.get(i) ;
			// mutate or extend:
			boolean extensionIsApplied = false ;
			if (sigma.size() < maxDepth && rnd.nextFloat() <= insertionProbability) {
				var tau = extend(sigma) ;
				if (tau != null && ! myPopulation.memberOf(tau)) {
				    newBatch.add(tau) ;
				    extensionIsApplied = true ;
				}
			}
			if (!extensionIsApplied && rnd.nextFloat() <= mutationProbability) {
				var tau = mutate(sigma) ;
				if (tau!= null && ! myPopulation.memberOf(tau))
					newBatch.add(tau) ;
			}
		}
		
		// clear the population; keeping only those that also appear in the new batch
		myPopulation.population.removeIf(CI -> ! newBatch.contains(CI.chromosome)) ;
		
		// now calculate the fitness of every member of the new-batch, and add it to the
		// population:
		for (var tau : newBatch) {
			if (myPopulation.memberOf(tau)) {
				// already in the population, no need to evaluate its fitness again
				continue ;
			}
			var info = fitnessValue(tau) ;
			totNumberOfEpisodes++ ;
			// evaluating fitness may shorten tau, or even throw away all its elements if the first
			// gene is not even executable; only add it to the population if it is not empty:
			if (! tau.isEmpty() && ! myPopulation.memberOf(tau))
				myPopulation.add(info);
			if (stopAfterGoalIsAchieved && goalHasBeenAchieved) break ;	
		}
		
		generationNr++ ;
	}
	
	@Override
	boolean terminationCondition() {
		return (this.stopAfterGoalIsAchieved && goalHasBeenAchieved) 
				|| remainingSearchBudget <= 0
				|| (maxNumberOfEpisodes != null && totNumberOfEpisodes > maxNumberOfEpisodes) 
				|| (maxNumberOfGenerations != null && generationNr < maxNumberOfGenerations);
	}

	/**
	 * This method is not available for this class.
	 */
	float runAlgorithmForOneEpisode() throws Exception {
		log("This method is not availabel for this class.") ;
		throw new UnsupportedOperationException("This method is not availabel for this class.") ;
	}
	
	@Override
	public AlgorithmResult runAlgorithm() throws Exception {
		if (maxPopulationSize <= 4)
			throw new IllegalArgumentException("maxPopulationSize should be at least 4.") ;
		
		long tstart = System.currentTimeMillis() ;
		remainingSearchBudget = totalSearchBudget ;
		createInitialPopulation() ;
		log(showStatus()) ;
	    if (knownInteractables.isEmpty())
	    	throw new IllegalArgumentException("The algorithm cannot find any action to activate.") ;
		this.remainingSearchBudget = this.remainingSearchBudget - (int) (System.currentTimeMillis()  - tstart) ;
		List<Float> episodesValues = new LinkedList<>() ;
		while (! terminationCondition()) {
			long t0 = System.currentTimeMillis() ;
			evolve() ;
			log(">>> EVOLUTION gen:" + generationNr) ;
			log(showStatus()) ;
			episodesValues.add(myPopulation.getBest().fitness) ;			
			long duration = System.currentTimeMillis() - t0 ;
			this.remainingSearchBudget = this.remainingSearchBudget - (int) duration ;
		}
		var R = new AlgorithmResult() ;
		R.algName = this.algName ;
		R.goalAchieved = goalHasBeenAchieved() ;
		R.totEpisodes = totNumberOfEpisodes ;
		R.usedBudget = totalSearchBudget - remainingSearchBudget ;
		R.usedTurns = turn ;
		R.winningplay = this.winningplay ;
		R.episodesValues = episodesValues ;
		log("*** END " + R.showShort());
			
		log(">>> remaining budget:" + remainingSearchBudget) ;
		log(">>> exec-time:" + (System.currentTimeMillis() - tstart)) ;
		
		return R ;
	}
	

}
