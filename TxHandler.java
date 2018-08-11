import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TxHandler {

    private UTXOPool currentUTXOPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(final UTXOPool utxoPool) {
        this.currentUTXOPool = new UTXOPool(utxoPool);
        System.out.println("Initialization done ");
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        if (currentUTXOPool == null) {
            return false;
        }

        boolean coinsPresentInUTXOPool = claimedUTXOsPresentInPool(tx);
        if (!coinsPresentInUTXOPool) {
            return false;
        }

        boolean signaturesValid = txSignaturesValid(tx);
        if (!signaturesValid) {
            return false;
        }

        boolean noUTXOClaimedMultipleTimes = noUTXOClaimedMultipleTimes(tx);
        if (!noUTXOClaimedMultipleTimes) {
            return false;
        }

        boolean allOutputValuesNonNegative = allOutputValuesNonNegative(tx);
        if (!allOutputValuesNonNegative) {
            return false;
        }

        boolean inputValuesGreaterThenOutputValues = inputValuesGreaterThenOutputValues(tx);
        return inputValuesGreaterThenOutputValues;
    }

    private boolean inputValuesGreaterThenOutputValues(final Transaction tx) {
        double inputValuesSum = 0;
        double outputValuesSum = 0;
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input txInput = tx.getInput(i);
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            Transaction.Output txOutputForInput = currentUTXOPool.getTxOutput(utxo);
            inputValuesSum += txOutputForInput.value;
        }
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output txOutput = tx.getOutput(i);
            outputValuesSum += txOutput.value;
        }
        if (inputValuesSum >= outputValuesSum) {
            return true;
        }
        return false;
    }

    private boolean allOutputValuesNonNegative(final Transaction tx) {
        boolean condition4 = true;
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output txOutput = tx.getOutput(i);
            double value = txOutput.value;
            condition4 &= (value >= 0);
            System.out.println("Transaction #" + i + ", txSignaturesValid " + condition4);
        }
        return condition4;
    }

    private boolean noUTXOClaimedMultipleTimes(final Transaction tx) {
        boolean condition3 = true;
        List<UTXO> utxos = new ArrayList<>();
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input txInput = tx.getInput(i);
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            condition3 &= !utxos.contains(utxo);
            System.out.println("Transaction #" + i + ", noUTXOClaimedMultipleTimes " + condition3);
            utxos.add(utxo);
        }
        return condition3;
    }

    private boolean claimedUTXOsPresentInPool(final Transaction tx) {
        boolean condition1 = true;
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input txInput = tx.getInput(i);
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            condition1 &= currentUTXOPool.contains(utxo);
            System.out.println("Transaction #" + i + ", claimedUTXOsPresentInPool " + condition1);
        }
        return condition1;
    }

    private boolean txSignaturesValid(final Transaction tx) {
        boolean condition2 = true;
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input txInput = tx.getInput(i);
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = txInput.signature;
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            Transaction.Output txOutputForInput = currentUTXOPool.getTxOutput(utxo);

            PublicKey pk = txOutputForInput.address;

            condition2 &= Crypto.verifySignature(pk, message, signature);
            System.out.println("Transaction #" + i + ", txSignaturesValid " + condition2);
        }
        return condition2;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> transactions = new ArrayList<>(Arrays.asList(possibleTxs));
        List<Transaction> executedTransactions = new ArrayList<>();

        for (Transaction transaction = getNextValidTransaction(transactions); transaction != null;
             transaction = getNextValidTransaction(transactions)) {
            System.out.println("Got Next valid transaction " + Arrays.toString(transaction.getHash()));
            transactions.remove(transaction);

            executeTransaction(transaction);
            executedTransactions.add(transaction);
        }
        return executedTransactions.toArray(new Transaction[0]);
    }

    /**
     * This method returns the current UTXO pool
     * @return current UTXO pool.
     */
    public UTXOPool getUTXOPool() {
        return currentUTXOPool;
    }

    private void executeTransaction(final Transaction transaction) {
        byte[] hash = transaction.getHash();

        for (int i = 0; i < transaction.numInputs(); i++) {
            Transaction.Input txInput = transaction.getInput(i);
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (currentUTXOPool.contains(utxo)) {
                currentUTXOPool.removeUTXO(utxo);
            } else {
                throw new RuntimeException("Invalid transaction for execution");
            }
        }

        for (int i = 0; i < transaction.numOutputs(); i++) {
            Transaction.Output txOutput = transaction.getOutput(i);
            UTXO updatedUtxo = new UTXO(hash, i);
            currentUTXOPool.addUTXO(updatedUtxo, txOutput);
        }
    }

    private Transaction getNextValidTransaction(final List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (isValidTx(transaction)) {
                return transaction;
            }
        }
        return null;
    }

}
