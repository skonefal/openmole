/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.plugin.executionprofiler.csv;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.openmole.core.implementation.mole.MoleExecutionProfiler;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import static org.openmole.plugin.executionprofiler.csv.internal.MoleJobInfoToColumns.toColumns;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class CSVProfiler extends MoleExecutionProfiler {

    final CSVWriter writer;

    public CSVProfiler(IMoleExecution moleExecution) {
        this(moleExecution, new OutputStreamWriter(System.out));
    }

    public CSVProfiler(IMoleExecution moleExecution, Writer out) {
        super(moleExecution);
        writer = new CSVWriter(out);
    }

    @Override
    protected void moleJobFinished(IMoleJob moleJob) throws InternalProcessingError, UserBadDataError {
        writer.writeNext(toColumns(moleJob));
        try {
            writer.flush();
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    protected void moleExecutionFinished() throws InternalProcessingError, UserBadDataError {
        try {
            writer.flush();
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    protected void moleExecutionStarting() throws InternalProcessingError, UserBadDataError {
        
    }
}
