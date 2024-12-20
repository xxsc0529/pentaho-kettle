/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.debug;

import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.RowAdapter;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;

/**
 * For a certain transformation, we want to be able to insert break-points into a transformation. These breakpoints can
 * be applied to steps. When a certain condition is met, the transformation will be paused and the caller will be
 * informed of this fact through a listener system.
 *
 * @author Matt
 *
 */
public class TransDebugMeta {

  public static final String XML_TAG = "trans-debug-meta";

  public static final String XML_TAG_STEP_DEBUG_METAS = "step-debug-metas";

  private TransMeta transMeta;
  private Map<StepMeta, StepDebugMeta> stepDebugMetaMap;

  public TransDebugMeta( TransMeta transMeta ) {
    this.transMeta = transMeta;
    stepDebugMetaMap = new HashMap<StepMeta, StepDebugMeta>();
  }

  /**
   * @return the referenced transformation metadata
   */
  public TransMeta getTransMeta() {
    return transMeta;
  }

  /**
   * @param transMeta
   *          the transformation metadata to reference
   */
  public void setTransMeta( TransMeta transMeta ) {
    this.transMeta = transMeta;
  }

  /**
   * @return the map that contains the debugging information per step
   */
  public Map<StepMeta, StepDebugMeta> getStepDebugMetaMap() {
    return stepDebugMetaMap;
  }

  /**
   * @param stepDebugMeta
   *          the map that contains the debugging information per step
   */
  public void setStepDebugMetaMap( Map<StepMeta, StepDebugMeta> stepDebugMeta ) {
    this.stepDebugMetaMap = stepDebugMeta;
  }

  public synchronized void addRowListenersToTransformation( final Trans trans ) {

    final TransDebugMeta self = this;

    // for every step in the map, add a row listener...
    //
    for ( final Map.Entry<StepMeta, StepDebugMeta> entry : stepDebugMetaMap.entrySet() ) {
      final StepMeta stepMeta = entry.getKey();
      final StepDebugMeta stepDebugMeta = entry.getValue();

      // What is the transformation thread to attach a listener to?
      //
      for ( StepInterface baseStep : trans.findBaseSteps( stepMeta.getName() ) ) {
        baseStep.addRowListener( new RowAdapter() {
          public void rowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException {
            rowWrittenEventHandler( rowMeta, row, stepDebugMeta, trans, self );
          }
        } );
      }
    }
  }

  @VisibleForTesting
  void rowWrittenEventHandler( RowMetaInterface rowMeta, Object[] row, StepDebugMeta stepDebugMeta, Trans trans,
                                     TransDebugMeta self ) throws KettleStepException {
    try {

      // This block of code is called whenever there is a row written by the step
      // So we want to execute the debugging actions that are specified by the step...
      //
      int rowCount = stepDebugMeta.getRowCount();

      if ( stepDebugMeta.isReadingFirstRows() && rowCount > 0 ) {

        int bufferSize = stepDebugMeta.getRowBuffer().size();
        if ( bufferSize < rowCount ) {

          // This is the classic preview mode.
          // We add simply add the row to the buffer.
          //
          stepDebugMeta.setRowBufferMeta( rowMeta );
          stepDebugMeta.getRowBuffer().add( rowMeta.cloneRow( row ) );
        } else {
          // pause the transformation...
          //
          if ( !trans.isPaused() ) {
            trans.pauseRunning();
            // Also call the pause / break-point listeners on the step debugger...
            //
            stepDebugMeta.fireBreakPointListeners( self );
          }
        }
      } else if ( stepDebugMeta.isPausingOnBreakPoint() && stepDebugMeta.getCondition() != null ) {
        // A break-point is set
        // Verify the condition and pause if required
        // Before we do that, see if a row count is set.
        // If so, keep the last rowCount rows in memory
        //
        if ( rowCount > 0 ) {
          // Keep a number of rows in memory
          // Store them in a reverse order to keep it intuitive for the user.
          //
          stepDebugMeta.setRowBufferMeta( rowMeta );
          stepDebugMeta.getRowBuffer().add( 0, rowMeta.cloneRow( row ) );

          // Only keep a number of rows in memory
          // If we have too many, remove the last (oldest)
          //
          int bufferSize = stepDebugMeta.getRowBuffer().size();
          if ( bufferSize > rowCount ) {
            stepDebugMeta.getRowBuffer().remove( bufferSize - 1 );
          }
        } else {
          // Just keep one row...
          //
          if ( stepDebugMeta.getRowBuffer().isEmpty() ) {
            stepDebugMeta.getRowBuffer().add( rowMeta.cloneRow( row ) );
          } else {
            stepDebugMeta.getRowBuffer().set( 0, rowMeta.cloneRow( row ) );
          }
        }

        // Now evaluate the condition and see if we need to pause the transformation
        //
        if ( stepDebugMeta.getCondition().evaluate( rowMeta, row ) ) {
          // We hit the break-point: pause the transformation
          //
          trans.pauseRunning();

          // Also fire off the break point listeners...
          //
          stepDebugMeta.fireBreakPointListeners( self );
        }
      }
    } catch ( KettleException e ) {
      throw new KettleStepException( e );
    }
  }

  /**
   * Add a break point listener to all defined step debug meta data
   *
   * @param breakPointListener
   *          the break point listener to add
   */
  public void addBreakPointListers( BreakPointListener breakPointListener ) {
    for ( StepDebugMeta stepDebugMeta : stepDebugMetaMap.values() ) {
      stepDebugMeta.addBreakPointListener( breakPointListener );
    }
  }

  /**
   * @return the number of times the break-point listeners got called. This is the total for all the steps.
   */
  public int getTotalNumberOfHits() {
    int total = 0;
    for ( StepDebugMeta stepDebugMeta : stepDebugMetaMap.values() ) {
      total += stepDebugMeta.getNumberOfHits();
    }
    return total;
  }

  /**
   * @return the number of steps used to preview or debug on
   */
  public int getNrOfUsedSteps() {
    int nr = 0;

    for ( StepDebugMeta stepDebugMeta : stepDebugMetaMap.values() ) {
      if ( stepDebugMeta.isReadingFirstRows() && stepDebugMeta.getRowCount() > 0 ) {
        nr++;
      } else if ( stepDebugMeta.isPausingOnBreakPoint()
        && stepDebugMeta.getCondition() != null && !stepDebugMeta.getCondition().isEmpty() ) {
        nr++;
      }
    }

    return nr;
  }
}
