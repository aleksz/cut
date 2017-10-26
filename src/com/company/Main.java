package com.company;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Main {

    private static final int MIN_LENGTH = 20;
    private static final int TIMEOUT = 10 * 60 * 1000;
    private static final int MAX_CUTS = 2;

    private static final List<Karkass> POOL = new ArrayList<>(asList(
            new Karkass(1, 50000)
    ));

    private static List<Integer> REQUIRED = required();
    private static long start = System.currentTimeMillis();
    private static int standardToBuy = 300;
    private static long leafs = 0;
    private static Solution bestSolution;

    private static class Karkass implements Cloneable {
        private int id;
        private int length;
        private int used;

        public Karkass(int id, int length) {
            this.id = id;
            this.length = length;
            this.used = 0;
        }

        public Karkass(Karkass karkass) {
            this.length = karkass.getLength();
            this.used = karkass.getUsed();
            this.id = karkass.id;
        }

        public void cut(int length) {
            this.used += length;
        }

        public int getLength() {
            return length;
        }

        public int getUsed() {
            return used;
        }

        public int getRamaining() {
            return length - used;
        }

        public List<Integer> proposeCuts(int max) {
            List<Integer> proposals = new ArrayList<>();

            for (int cut = MIN_LENGTH; cut <= getRamaining() && cut <= max; cut += 1) {
                proposals.add(cut);
            }

            return proposals;
        }

        @Override
        public Karkass clone() {
            return new Karkass(this);
        }

        @Override
        public boolean equals(Object obj) {
            return ((Karkass) obj).id == this.id;
        }

        @Override
        public String toString() {
            return "L:" + length + " U:" + used;
        }
    }

    private static class Cuts implements Cloneable {
        private List<Integer> lengths = new ArrayList<>();
        private int totalLength = 0;
        private boolean complete;

        public void add(int length, int required) {
            this.lengths.add(length);
            this.totalLength += length;
            this.complete = required == this.totalLength;
        }

        public Cuts() {

        }

        public Cuts(Cuts cuts) {
            this.lengths = new ArrayList<>(cuts.lengths);
            this.totalLength = cuts.totalLength;
            this.complete = cuts.isComplete();
        }

        public boolean isComplete() {
            return complete;
        }

        public int getTotalLength() {
            return this.totalLength;
        }

        public boolean canCutMore() {
            return this.lengths.size() < (MAX_CUTS + 1);
        }

        @Override
        public String toString() {
            return lengths.toString() + (complete ? "*" : "");
        }

        @Override
        public Cuts clone() {
            return new Cuts(this);
        }
    }

    private static class Solution implements Comparable, Cloneable {

        Integer cost = 0;
        List<Cuts> assigned = new ArrayList<>(REQUIRED.size());
        Map<Integer, Karkass> pool = new HashMap<>();
        int complete = 0;

        public Solution() {
            REQUIRED.forEach(r -> {
                    assigned.add(new Cuts());
            });
            this.pool = POOL.stream().map(k -> k.clone()).collect(toMap(k -> k.id, k -> k));
        }

        public Solution(Solution solution) {
            this.cost = solution.cost;
            this.pool = solution.pool.entrySet().stream().map(k -> k.getValue().clone()).collect(toMap(k -> k.id, k -> k));
            this.assigned = solution.assigned.stream().map(a -> a.clone()).collect(toList());
            this.complete = solution.complete;
        }

        public void cut(Karkass itemInPool, int length, int toPosition) {
            cost++;
            pool.get(itemInPool.id).cut(length);
//            pool.stream().filter(p -> p.equals(itemInPool)).findFirst().get().cut(length);
            assigned.get(toPosition).add(length, REQUIRED.get(toPosition));
            if (assigned.get(toPosition).isComplete()) {
                complete++;
            }
        }

//        private void add(int position) {
//
//        }
//
//        private void buy(int position) {
//            karkasses.get(position).add(standardToBuy);
//            this.cost++;
//        }

        @Override
        public int compareTo(Object o) {
            return this.eval().compareTo(((Solution) o).eval());
        }

        public Integer eval() {
            return cost;
        }

        private int numberOfSegments() {
            return assigned.stream().map(k -> k.lengths.size()).reduce(0, Integer::sum);
        }

        private boolean complete() {
            return complete == REQUIRED.size();
//            return assigned.stream().allMatch(c -> c.isComplete());
        }

        @Override
        protected Solution clone() {
            return new Solution(this);
        }

        @Override
        public String toString() {
            return "A: " + assigned + "P: " + pool;
        }
    }

    public static void main(String[] args) {

        System.out.println("Required: " + REQUIRED);
        System.out.println("Total length: " + REQUIRED.stream().reduce(0, Integer::sum));
        long start = System.currentTimeMillis();

        Solution solution = new Solution();

        propose(solution);

        if (leafs > 0) {
            System.out.println("TIMEOUT -> " + leafs + " leafs");
        }

        if (bestSolution == null) {
            System.out.println("SORRY");
            return;
        }

        System.out.println("Best option: " + bestSolution.cost + " " + bestSolution.assigned);
        System.out.println("" + (System.currentTimeMillis() - start) / 1000 + "s");
    }

    private static List<Integer> required() {
        try {
            return new BufferedReader(new FileReader("required.csv")).lines().map(l -> {
				 String[] parts = l.split(",");
				 int length = new Integer(parts[0]);
				 int number = new Integer(parts[1]);

				List<Integer> required = new ArrayList<>(number);
				for (int i = 0; i < number; i++) {
					required.add(length);
				}

				return required;
			}).flatMap(x -> x.stream()).collect(toList());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void propose(Solution solution) {

//        System.out.println(solution);


        if (solution.complete()) {
            if (bestSolution == null || solution.eval() < bestSolution.eval()) {
                bestSolution = solution;
            }
            return;
        }

        if (System.currentTimeMillis() - start > TIMEOUT) {
//            System.out.println("TIMEOUT");
//            System.out.println(solution);
            leafs++;
            return;
        }

        for (int i = 0; i < solution.assigned.size(); i++ ) {

            if (!solution.assigned.get(i).isComplete() && solution.assigned.get(i).canCutMore()) {

                if (bestSolution != null && solution.eval() + 1 > bestSolution.eval()) {
//                    System.out.println("Dead end for " + solution + ", already have " + bestSolution);
                    continue;
                }

                int finalI1 = i;
                solution.pool.values().forEach(available -> {
                    int finalI = finalI1;

                    int remaining = REQUIRED.get(finalI) - solution.assigned.get(finalI).getTotalLength();

                    available.proposeCuts(remaining).forEach(cut -> {
                        Solution child = solution.clone();
                        child.cut(available, cut, finalI);
                        propose(child);
                    });
                });
            }
        }

    }
}