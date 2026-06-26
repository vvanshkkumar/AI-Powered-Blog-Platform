import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  CardBody,
  CardHeader,
  Button,
  useDisclosure,
} from '@nextui-org/react';
import { ArrowLeft, Sparkles } from 'lucide-react';
import { apiService, Post, Category, Tag, PostStatus } from '../services/apiService';
import PostForm from '../components/PostForm';
import GenerateModal from '../components/GenerateModal';

const EditPostPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [post, setPost] = useState<Post | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // AI generation modal
  const { isOpen, onOpen, onClose } = useDisclosure();

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [categoriesResponse, tagsResponse] = await Promise.all([
          apiService.getCategories(),
          apiService.getTags()
        ]);

        setCategories(categoriesResponse);
        setTags(tagsResponse);

        if (id) {
          const postResponse = await apiService.getPost(id);
          setPost(postResponse);
        }

        setError(null);
      } catch (err) {
        setError('Failed to load necessary data. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);

  const handleSubmit = async (postData: {
    title: string;
    content: string;
    categoryId: string;
    tagIds: string[];
    status: PostStatus;
  }) => {
    try {
      setIsSubmitting(true);
      setError(null);

      if (id) {
        await apiService.updatePost(id, {
          ...postData,
          id
        });
      } else {
        await apiService.createPost(postData);
      }

      navigate('/');
    } catch (err) {
      setError('Failed to save the post. Please try again.');
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    if (id) {
      navigate(`/posts/${id}`);
    } else {
      navigate('/');
    }
  };

  const handleAiGenerated = (result: {
    title: string;
    content: string;
    suggestedTags: string[];
  }) => {
    // Match suggested tag names to existing DB tags (case-insensitive)
    const matchedTagObjects = tags.filter(t =>
      result.suggestedTags.some(s =>
        s.toLowerCase() === t.name.toLowerCase()
      )
    );

    // Build a post object to pre-fill the form
    setPost({
      id: id || '',
      title: result.title,
      content: result.content,
      category: post?.category || { id: '', name: '', postCount: 0 },
      tags: matchedTagObjects,
      author: post?.author || { id: '', name: '' },
      status: 'DRAFT' as PostStatus,
      createdAt: post?.createdAt || new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4">
        <Card className="w-full animate-pulse">
          <CardBody>
            <div className="h-8 bg-default-200 rounded w-3/4 mb-4"></div>
            <div className="space-y-3">
              <div className="h-4 bg-default-200 rounded w-full"></div>
              <div className="h-4 bg-default-200 rounded w-full"></div>
              <div className="h-4 bg-default-200 rounded w-2/3"></div>
            </div>
          </CardBody>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4">
      <Card className="w-full">
        <CardHeader className="flex justify-between items-center">
          <div className="flex items-center gap-4">
            <Button
              variant="flat"
              startContent={<ArrowLeft size={16} />}
              onClick={handleCancel}
            >
              Back
            </Button>
            <h1 className="text-2xl font-bold">
              {id ? 'Edit Post' : 'Create New Post'}
            </h1>
          </div>
          {!id && (
            <Button
              color="secondary"
              variant="flat"
              startContent={<Sparkles size={16} />}
              onPress={onOpen}
            >
              Generate with AI
            </Button>
          )}
        </CardHeader>

        <CardBody>
          {error && (
            <div className="mb-4 p-4 text-red-500 bg-red-50 rounded-lg">
              {error}
            </div>
          )}

          <PostForm
            initialPost={post}
            onSubmit={handleSubmit}
            onCancel={handleCancel}
            categories={categories}
            availableTags={tags}
            isSubmitting={isSubmitting}
          />
        </CardBody>
      </Card>

      <GenerateModal
        isOpen={isOpen}
        onClose={onClose}
        onGenerated={handleAiGenerated}
      />
    </div>
  );
};

export default EditPostPage;