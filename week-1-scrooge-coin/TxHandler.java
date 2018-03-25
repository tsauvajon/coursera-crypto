import java.util.HashSet;

public class TxHandler {
    private UTXOPool pool;


    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        UTXOPool validPool = new UTXOPool();

        double outputSum = 0;
        double inputSum = 0;

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }

            outputSum += output.value;
        }

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, i);
            Transaction.Output output = pool.getTxOutput(utxo);

            if (validPool.contains(utxo)) {
                return false;
            }

            if (!pool.contains(utxo)) {
                return false;
            }

            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            inputSum += output.value;
            validPool.addUTXO(utxo, output);
        }

        return outputSum <= inputSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        HashSet<Transaction> txs = new HashSet<>();

        for (Transaction tx : possibleTxs) {
            if (!isValidTx(tx)) {
                continue;
            }

            for (int i = 0; i < tx.numOutputs(); i++) {
                UTXO utxo = new UTXO(tx.getHash(), i);
                pool.addUTXO(utxo, tx.getOutput(i));
            }

            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                pool.removeUTXO(utxo);
            }
        }

        return txs.toArray(new Transaction[txs.size()]);
    }

}
