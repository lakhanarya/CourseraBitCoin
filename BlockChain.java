// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.List;
import java.util.Map;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private Block genesisBlock;
    private ByteArrayWrapper headBlockHash;
    private Block headBlock;
    private TransactionPool transactionPool;
    private UTXOPool currentUTXOPool;
    private Map<ByteArrayWrapper, Block> blockChain;

    private int heightOfBlockChain = 0;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        this.genesisBlock = genesisBlock;
        this.headBlock = genesisBlock;
        this.headBlockHash = new ByteArrayWrapper(genesisBlock.getHash());
        this.heightOfBlockChain = 1;
        this.transactionPool = new TransactionPool();
        this.currentUTXOPool = new UTXOPool();

        Transaction coinbaseTransaction = genesisBlock.getCoinbase();
        List<Transaction> genesisTransactions = genesisBlock.getTransactions(); // Assuming empty for now.

        for (int i = 0; i < coinbaseTransaction.numOutputs(); i++) {
            Transaction.Output txOutput = coinbaseTransaction.getOutput(i);
            UTXO updatedUtxo = new UTXO(coinbaseTransaction.getHash(), i);
            currentUTXOPool.addUTXO(updatedUtxo, txOutput);
        }
        blockChain.put(headBlockHash, genesisBlock);
        transactionPool.addTransaction(coinbaseTransaction);
    }

    /**
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return headBlock;
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return currentUTXOPool;
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        if (block.getPrevBlockHash() == null) {
            return false;
        } else if(block.getPrevBlockHash() != headBlock.getHash()) {
            return false;
        }

        headBlock = block;
        Transaction coinbaseTransaction = block.getCoinbase();
        List<Transaction> transactions = block.getTransactions();
        TxHandler txHandler = new TxHandler(currentUTXOPool);

        Transaction[] executedTransactions = txHandler.handleTxs(transactions.toArray(new Transaction[0]));
        if(executedTransactions.length != transactions.size()) {
            // All transactions are not valid
            return false;
        }
        currentUTXOPool = txHandler.getUTXOPool();

        for (int i = 0; i < coinbaseTransaction.numOutputs(); i++) {
            Transaction.Output txOutput = coinbaseTransaction.getOutput(i);
            UTXO updatedUtxo = new UTXO(coinbaseTransaction.getHash(), i);
            currentUTXOPool.addUTXO(updatedUtxo, txOutput);
        }
        transactionPool.addTransaction(coinbaseTransaction);
        heightOfBlockChain++;
        ByteArrayWrapper blockHash = new ByteArrayWrapper(block.getHash());
        blockChain.put(blockHash, block);
        return true;
    }

    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        transactionPool.addTransaction(tx);
    }
}