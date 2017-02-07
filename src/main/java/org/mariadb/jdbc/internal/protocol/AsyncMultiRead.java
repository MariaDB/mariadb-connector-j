package org.mariadb.jdbc.internal.protocol;

import org.mariadb.jdbc.internal.packet.ComStmtPrepare;
import org.mariadb.jdbc.internal.packet.dao.parameters.ParameterHolder;
import org.mariadb.jdbc.internal.queryresults.Results;
import org.mariadb.jdbc.internal.util.dao.PrepareResult;
import org.mariadb.jdbc.internal.util.dao.QueryException;

import java.util.List;
import java.util.concurrent.Callable;

import static org.mariadb.jdbc.internal.util.SqlStates.INTERRUPTED_EXCEPTION;

public class AsyncMultiRead implements Callable<AsyncMultiReadResult> {

    private final ComStmtPrepare comStmtPrepare;
    private final int nbResult;
    private final int sendCmdCounter;
    private final Protocol protocol;
    private final boolean readPrepareStmtResult;
    private Results results;
    private boolean binaryProtocol;
    private final AbstractMultiSend bulkSend;
    private int paramCount;
    private final List<ParameterHolder[]> parametersList;
    private final List<String> queries;
    private AsyncMultiReadResult asyncMultiReadResult;


    /**
     * Read results async to avoid local and remote networking stack buffer overflow "lock".
     *
     * @param comStmtPrepare current prepare
     * @param nbResult number of command send
     * @param sendCmdCounter initial command counter
     * @param protocol protocol
     * @param readPrepareStmtResult must read prepare statement result
     * @param bulkSend bulk sender object
     * @param paramCount number of parameters
     * @param binaryProtocol using binary protocol
     * @param results execution result
     * @param parametersList parameter list
     * @param queries queries
     * @param prepareResult prepare result
     */
    public AsyncMultiRead(ComStmtPrepare comStmtPrepare, int nbResult, int sendCmdCounter,
                          Protocol protocol, boolean readPrepareStmtResult, AbstractMultiSend bulkSend, int paramCount,
                          boolean binaryProtocol, Results results,
                          List<ParameterHolder[]> parametersList, List<String> queries, PrepareResult prepareResult) {
        this.comStmtPrepare = comStmtPrepare;
        this.nbResult = nbResult;
        this.sendCmdCounter = sendCmdCounter;
        this.protocol = protocol;
        this.readPrepareStmtResult = readPrepareStmtResult;
        this.bulkSend = bulkSend;
        this.paramCount = paramCount;
        this.binaryProtocol = binaryProtocol;
        this.results = results;
        this.parametersList = parametersList;
        this.queries = queries;
        this.asyncMultiReadResult = new AsyncMultiReadResult(prepareResult);
    }

    @Override
    public AsyncMultiReadResult call() throws Exception {
        // avoid synchronisation of calls for write and read
        // since technically, getResult can be called before the write is send.
        // Other solution would have been to synchronised write and read, but would have been less performant,
        // just to have this timeout according to set value
        if (protocol.getOptions().socketTimeout != null)  protocol.changeSocketSoTimeout(0);

        if (readPrepareStmtResult) {
            try {
                asyncMultiReadResult.setPrepareResult(comStmtPrepare.read(protocol.getPacketFetcher()));
            } catch (QueryException queryException) {
                asyncMultiReadResult.setException(queryException);
            }
        }

        //read all corresponding results
        for (int counter = 0; counter < nbResult; counter++) {
            try {
                protocol.getResult(results);
            } catch (QueryException qex) {
                if (asyncMultiReadResult.getException() == null) {
                    asyncMultiReadResult.setException(bulkSend.handleResultException(qex, results,
                            parametersList, queries, counter, sendCmdCounter, paramCount,
                            asyncMultiReadResult.getPrepareResult()));
                }
            }
        }
        if (protocol.getOptions().socketTimeout != null)  protocol.changeSocketSoTimeout(protocol.getOptions().socketTimeout);

        return asyncMultiReadResult;
    }

}
