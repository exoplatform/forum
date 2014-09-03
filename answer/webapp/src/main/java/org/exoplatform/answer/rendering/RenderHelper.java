/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.answer.rendering;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.Question;
import org.exoplatform.forum.common.webui.BuildRendering.AbstractRenderDelegate;
import org.exoplatform.forum.rendering.MarkupRenderingService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class RenderHelper {
  protected static final Log LOG = ExoLogger.getLogger(RenderHelper.class);
  private MarkupRenderingService markupRenderingService;
  private static final AbstractRenderDelegate<Answer>   ANSWER_DELEGATE   = new AbstractRenderDelegate<Answer>() {
    @Override
    public String getMessage(Answer answer) {
      return answer.getResponses();
    }
  };
  private static final AbstractRenderDelegate<Comment>  COMMENT_DELEGATE  = new AbstractRenderDelegate<Comment>() {
    @Override
    public String getMessage(Comment comment) {
      return comment.getComments();
    }
  };
  private static final AbstractRenderDelegate<Question> QUESTION_DELEGATE = new AbstractRenderDelegate<Question>() {
    @Override
    public String getMessage(Question question) {
      return question.getDetail();
    }
  };

  public RenderHelper() {
  }

  /**
   * Render markup for a forum answer

   * @param answer
   * @return The content of answer has processed.
   */
  public String renderAnswer(Answer answer) {
    return delegateRendering(ANSWER_DELEGATE, answer);
  }
  /**
   * Render markup for a forum comment

   * @param comment
   * @return The content of comment has processed.
   */
  public String renderComment(Comment comment) {
    return delegateRendering(COMMENT_DELEGATE, comment);
  }
  /**
   * Render markup for a forum question

   * @param question
   * @return The content of question has processed.
   */
  public String renderQuestion(Question question) {
    return delegateRendering(QUESTION_DELEGATE, question);
  }

  private <T> String delegateRendering(AbstractRenderDelegate<T> delegate, T target) {
    try {
      return getMarkupRenderingService().delegateRendering(delegate, target);
    } catch (Exception e) {
      LOG.warn("Failed to render " + target.getClass().getSimpleName());
      LOG.debug(e.getMessage(), e);
      return delegate.getMessage(target);
    }
  }

  /**
   * Get the @MarkupRenderingService
   * @return
   */
  public MarkupRenderingService getMarkupRenderingService() {
    if (markupRenderingService == null) {
      markupRenderingService = CommonsUtils.getService(MarkupRenderingService.class);
    }
    return markupRenderingService;
  }

  /**
   *  Set the @MarkupRenderingService
   * @param service
   */
  public void setMarkupRenderingService(MarkupRenderingService service) {
    this.markupRenderingService = service;
  }
}
